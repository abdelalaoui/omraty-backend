package com.omraty.backend.service;

import com.omraty.backend.entities.OfferFormula;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.OfferFormulaException;
import com.omraty.backend.repository.OfferFormulaRepository;
import com.omraty.backend.repository.PackagePromoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartes de l'écran « خدماتنا » de l'offre BED_OFFER (voir migration V64), gérées depuis l'admin :
 * l'app affiche la liste renvoyée par {@link #getFormulasForPackage} et choisit le parcours d'après
 * l'action de chaque carte. Chaque carte a son prix par départ (grille offer_formula_price, voir
 * migration V66) : chambre entière pour 1/2/3 places, un lit pour la chambre de 5.
 */
@Service
public class OfferFormulaService {

    private static final int MAX_LABEL_LENGTH = 255;
    private static final Set<Integer> FAMILY_ROOM_CAPACITIES = Set.of(2, 3, 5);

    /** Carte prête à afficher pour un package : son prix est null pour CONTACT. */
    public record PricedOfferFormula(OfferFormula formula, BigDecimal price) {}

    private final OfferFormulaRepository offerFormulaRepository;
    private final PackagePromoRepository packagePromoRepository;
    private final PromoPackageService promoPackageService;

    public OfferFormulaService(
            OfferFormulaRepository offerFormulaRepository,
            PackagePromoRepository packagePromoRepository,
            PromoPackageService promoPackageService) {
        this.offerFormulaRepository = offerFormulaRepository;
        this.packagePromoRepository = packagePromoRepository;
        this.promoPackageService = promoPackageService;
    }

    /**
     * Cartes visibles de ce mode pour ce package promo, avec leur prix pour ce départ. Une carte
     * sans prix pour ce départ n'est pas proposée, sauf CONTACT. Vide si l'offre est coupée ou si
     * le package n'est pas proposé dans l'offre (masqué ou pas en promo).
     */
    public List<PricedOfferFormula> getFormulasForPackage(long packageId, OfferParty party) {
        boolean bookable =
                promoPackageService.isOfferEnabled()
                        && packagePromoRepository
                                .findByPackageId(packageId)
                                .filter(PromoPackage::visible)
                                .isPresent();
        if (!bookable) {
            return List.of();
        }
        Map<Long, BigDecimal> prices = offerFormulaRepository.findPricesByPackage(packageId);
        return offerFormulaRepository.findVisibleByParty(party).stream()
                .map(formula -> new PricedOfferFormula(formula, prices.get(formula.id())))
                .filter(
                        priced ->
                                priced.formula().action() == OfferFormulaAction.CONTACT
                                        || priced.price() != null)
                .toList();
    }

    /**
     * Prix réellement facturé pour une réservation via la carte formulaId sur ce départ (voir
     * RoomService.reserveOfferBed, reserveOfferBeds, reserveOfferPrivateRoom). formulaId null
     * (versions de l'app antérieures aux cartes) : première carte visible de cette action et de
     * cette capacité.
     *
     * @throws BookingPaymentException.PriceNotConfiguredException si l'offre est coupée ou si la
     *     carte n'a pas de prix pour ce départ.
     * @throws com.omraty.backend.exception.PromoPackageException.PackageNotInOfferException si le
     *     package n'est pas proposé dans l'offre.
     * @throws OfferFormulaException.InvalidOfferFormulaRequestException si la carte est masquée ou
     *     ne correspond pas à ce type de réservation.
     */
    public BigDecimal resolvePrice(
            long packageId, Long formulaId, OfferFormulaAction action, int capacity) {
        promoPackageService.ensureBookable(packageId);
        OfferFormula formula =
                formulaId != null
                        ? offerFormulaRepository
                                .findById(formulaId)
                                .orElseThrow(() -> notFound(formulaId))
                        : offerFormulaRepository.findAll().stream()
                                .filter(OfferFormula::visible)
                                .filter(candidate -> candidate.action() == action)
                                .filter(candidate -> Objects.equals(candidate.capacity(), capacity))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new BookingPaymentException
                                                        .PriceNotConfiguredException(
                                                        "Cette formule n'est pas proposée"
                                                                + " actuellement"));
        if (!formula.visible()
                || formula.action() != action
                || !Objects.equals(formula.capacity(), capacity)) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Cette carte ne correspond pas à cette réservation");
        }
        BigDecimal price = offerFormulaRepository.findPricesByPackage(packageId).get(formula.id());
        if (price == null) {
            throw new BookingPaymentException.PriceNotConfiguredException(
                    "Cette formule n'a pas de prix pour ce départ");
        }
        return price;
    }

    // --- Administration ---

    public List<OfferFormula> getAllFormulas() {
        return offerFormulaRepository.findAll();
    }

    /** Grille complète des prix, indexée par carte puis par packageId. */
    public Map<Long, Map<Long, BigDecimal>> getAllPrices() {
        return offerFormulaRepository.findAllPrices();
    }

    public Map<Long, BigDecimal> getPrices(long formulaId) {
        return offerFormulaRepository.findPricesByFormula(formulaId);
    }

    /**
     * Ajoute une carte. capacity doit correspondre à l'action : 5 pour SHARED_BED, 1 pour
     * PRIVATE_ROOM, 2/3/5 pour FAMILY_ROOM, aucune pour CONTACT (même règle que la contrainte
     * chk_offer_formula_capacity, vérifiée ici pour renvoyer un 400 explicite). prices : prix par
     * packageId (voir {@link #applyPrices}), optionnel.
     */
    @Transactional
    public OfferFormula createFormula(
            OfferParty party,
            OfferFormulaAction action,
            Integer capacity,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible,
            Map<Long, BigDecimal> prices) {
        if (party == null || action == null) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Le mode et l'action sont requis");
        }
        validateCapacity(action, capacity);
        validateLabel(labelFr, "labelFr");
        validateLabel(labelEn, "labelEn");
        validateLabel(labelAr, "labelAr");
        OfferFormula created =
                offerFormulaRepository.insert(
                        party,
                        action,
                        capacity,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder != null ? displayOrder : 0,
                        visible == null || visible);
        applyPrices(created, prices);
        return created;
    }

    /** Libellés, ordre, visibilité et prix par départ ; null = inchangé. */
    @Transactional
    public OfferFormula updateFormula(
            long id,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible,
            Map<Long, BigDecimal> prices) {
        if (labelFr != null) {
            validateLabel(labelFr, "labelFr");
        }
        if (labelEn != null) {
            validateLabel(labelEn, "labelEn");
        }
        if (labelAr != null) {
            validateLabel(labelAr, "labelAr");
        }
        OfferFormula updated =
                offerFormulaRepository
                        .update(id, labelFr, labelEn, labelAr, displayOrder, visible)
                        .orElseThrow(() -> notFound(id));
        applyPrices(updated, prices);
        return updated;
    }

    public void deleteFormula(long id) {
        if (!offerFormulaRepository.deleteById(id)) {
            throw notFound(id);
        }
    }

    /**
     * prices : packageId -> prix (null = retirer le prix de ce départ, la carte n'y est plus
     * proposée). Seuls les départs listés changent ; une carte CONTACT n'a jamais de prix.
     */
    private void applyPrices(OfferFormula formula, Map<Long, BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> entry : prices.entrySet()) {
            long packageId = entry.getKey();
            BigDecimal price = entry.getValue();
            if (price == null) {
                offerFormulaRepository.deletePrice(formula.id(), packageId);
                continue;
            }
            if (formula.action() == OfferFormulaAction.CONTACT) {
                throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                        "Une carte « contact » n'a pas de prix");
            }
            if (price.signum() <= 0) {
                throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                        "Le prix doit être supérieur à 0");
            }
            if (!packagePromoRepository.isPromo(packageId)) {
                throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                        "Le package " + packageId + " ne fait pas partie de l'offre");
            }
            offerFormulaRepository.upsertPrice(formula.id(), packageId, price);
        }
    }

    private static OfferFormulaException.OfferFormulaNotFoundException notFound(long id) {
        return new OfferFormulaException.OfferFormulaNotFoundException(
                "Carte de l'offre introuvable (id=" + id + ")");
    }

    private static void validateCapacity(OfferFormulaAction action, Integer capacity) {
        boolean valid =
                switch (action) {
                    case SHARED_BED -> capacity != null && capacity == 5;
                    case PRIVATE_ROOM -> capacity != null && capacity == 1;
                    case FAMILY_ROOM -> capacity != null
                            && FAMILY_ROOM_CAPACITIES.contains(capacity);
                    case CONTACT -> capacity == null;
                };
        if (!valid) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Capacité invalide pour "
                            + action
                            + " : 5 (SHARED_BED), 1 (PRIVATE_ROOM), 2/3/5 (FAMILY_ROOM),"
                            + " aucune (CONTACT)");
        }
    }

    private static void validateLabel(String label, String fieldName) {
        if (label == null || label.isBlank()) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Le champ " + fieldName + " ne peut pas être vide");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + MAX_LABEL_LENGTH
                            + ")");
        }
    }
}
