package com.omraty.backend.service;

import com.omraty.backend.entities.OfferFormula;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import com.omraty.backend.exception.OfferFormulaException;
import com.omraty.backend.repository.OfferFormulaRepository;
import com.omraty.backend.repository.PackagePromoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Cartes de l'écran « خدماتنا » de l'offre BED_OFFER (voir migration V64), gérées depuis l'admin :
 * l'app affiche la liste renvoyée par {@link #getFormulasForPackage} et choisit le parcours d'après
 * l'action de chaque carte.
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
     * Cartes visibles de ce mode pour ce package promo, avec leur prix (celui du package pour la
     * capacity de la carte). Une carte sans prix pour ce package n'est pas proposée, sauf CONTACT.
     * Vide si l'offre est coupée ou si le package n'est pas proposé dans l'offre (masqué ou pas en
     * promo).
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
        Map<Integer, BigDecimal> prices = packagePromoRepository.findPricesByType(packageId);
        return offerFormulaRepository.findVisibleByParty(party).stream()
                .map(formula -> new PricedOfferFormula(formula, priceOf(formula, prices)))
                .filter(
                        priced ->
                                priced.formula().action() == OfferFormulaAction.CONTACT
                                        || priced.price() != null)
                .toList();
    }

    private static BigDecimal priceOf(OfferFormula formula, Map<Integer, BigDecimal> prices) {
        return formula.capacity() == null ? null : prices.get(formula.capacity());
    }

    // --- Administration ---

    public List<OfferFormula> getAllFormulas() {
        return offerFormulaRepository.findAll();
    }

    /**
     * Ajoute une carte. capacity doit correspondre à l'action : 5 pour SHARED_BED, 1 pour
     * PRIVATE_ROOM, 2/3/5 pour FAMILY_ROOM, aucune pour CONTACT (même règle que la contrainte
     * chk_offer_formula_capacity, vérifiée ici pour renvoyer un 400 explicite).
     */
    public OfferFormula createFormula(
            OfferParty party,
            OfferFormulaAction action,
            Integer capacity,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible) {
        if (party == null || action == null) {
            throw new OfferFormulaException.InvalidOfferFormulaRequestException(
                    "Le mode et l'action sont requis");
        }
        validateCapacity(action, capacity);
        validateLabel(labelFr, "labelFr");
        validateLabel(labelEn, "labelEn");
        validateLabel(labelAr, "labelAr");
        return offerFormulaRepository.insert(
                party,
                action,
                capacity,
                labelFr,
                labelEn,
                labelAr,
                displayOrder != null ? displayOrder : 0,
                visible == null || visible);
    }

    /** Libellés, ordre et visibilité ; null = inchangé. */
    public OfferFormula updateFormula(
            long id,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible) {
        if (labelFr != null) {
            validateLabel(labelFr, "labelFr");
        }
        if (labelEn != null) {
            validateLabel(labelEn, "labelEn");
        }
        if (labelAr != null) {
            validateLabel(labelAr, "labelAr");
        }
        return offerFormulaRepository
                .update(id, labelFr, labelEn, labelAr, displayOrder, visible)
                .orElseThrow(() -> notFound(id));
    }

    public void deleteFormula(long id) {
        if (!offerFormulaRepository.deleteById(id)) {
            throw notFound(id);
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
