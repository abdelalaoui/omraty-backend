package com.omraty.backend.service;

import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.repository.PackagePromoRepository;
import com.omraty.backend.repository.PackageRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Packages promo de la bannière BED_OFFER (voir migration V57) : des packages Omra ordinaires —
 * mêmes dates, chambres, lits, paiements, achats et factures — à deux différences près :
 *
 * <ul>
 *   <li>ils ne sont proposés que depuis la bannière ({@link #getBookablePackages}) et ne se
 *       réservent que par les routes de l'offre (RoomService.reserveOfferBed...) : le parcours Omra
 *       normal les ignore (voir PackageService.getReservationGroups) et les refuse ({@link
 *       #ensureNotPromo}) ;
 *   <li>leurs prix sont ceux des cartes de l'offre pour ce départ (voir migration V66,
 *       OfferFormulaService) au lieu du tarif global des formules.
 * </ul>
 *
 * Le réglage bed_offer_enabled (V47) reste l'interrupteur général de l'offre.
 */
@Service
public class PromoPackageService {

    static final String BED_OFFER_ENABLED_KEY = "bed_offer_enabled";

    private final PackagePromoRepository packagePromoRepository;
    private final PackageRepository packageRepository;
    private final AppSettingService appSettingService;

    public PromoPackageService(
            PackagePromoRepository packagePromoRepository,
            PackageRepository packageRepository,
            AppSettingService appSettingService) {
        this.packagePromoRepository = packagePromoRepository;
        this.packageRepository = packageRepository;
        this.appSettingService = appSettingService;
    }

    public boolean isOfferEnabled() {
        return Boolean.parseBoolean(
                appSettingService.getSetting(BED_OFFER_ENABLED_KEY).value().trim());
    }

    /**
     * Packages proposés depuis la bannière (visibles, avec une carte proposée) ; vide si coupée.
     */
    public List<PromoPackage> getBookablePackages() {
        return isOfferEnabled() ? packagePromoRepository.findBookable() : List.of();
    }

    /**
     * Prix de lit le plus bas des packages proposés (carte « lit en chambre de 5 »), pour le
     * ctaPrice de la bannière (voir BannerController) : les versions de l'app antérieures aux
     * packages promo n'ouvrent l'offre que si ce prix est renseigné. Vide si l'offre est coupée ou
     * sans lit à prix.
     */
    public Optional<BigDecimal> getMinimumBedPrice() {
        return isOfferEnabled()
                ? packagePromoRepository.findMinBookableBedPrice()
                : Optional.empty();
    }

    /**
     * Le départ peut-il être réservé via l'offre (voir OfferFormulaService.resolvePrice) ?
     *
     * @throws BookingPaymentException.PriceNotConfiguredException si l'offre est coupée.
     * @throws PromoPackageException.PackageNotInOfferException si le package n'est pas proposé dans
     *     l'offre (pas en promo ou masqué).
     */
    public void ensureBookable(long packageId) {
        if (!isOfferEnabled()) {
            throw new BookingPaymentException.PriceNotConfiguredException(
                    "L'offre spéciale n'est pas active actuellement");
        }
        packagePromoRepository
                .findByPackageId(packageId)
                .filter(PromoPackage::visible)
                .orElseThrow(
                        () ->
                                new PromoPackageException.PackageNotInOfferException(
                                        "Ce package ne fait pas partie de l'offre"));
    }

    /**
     * Les packages promo ne se réservent que depuis la bannière (prix des cartes, paiement complet)
     * : refuse les routes de réservation du parcours Omra normal.
     */
    public void ensureNotPromo(long packageId) {
        if (packagePromoRepository.isPromo(packageId)) {
            throw new PromoPackageException.PromoPackageNotBookableException(
                    "Ce package est réservé à l'offre spéciale");
        }
    }

    // --- Administration ---

    public List<PromoPackage> getAllPromoPackages() {
        return packagePromoRepository.findAll();
    }

    /**
     * Met un package Omra existant en promo, ou modifie sa promo : visible/displayOrder optionnels
     * (valeur existante, ou visible et en tête à l'ajout). Ses prix se saisissent ensuite sur les
     * cartes de l'offre.
     */
    @Transactional
    public PromoPackage upsertPromoPackage(long packageId, Boolean visible, Integer displayOrder) {
        packageRepository
                .findById(packageId)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + packageId + ")"));
        packagePromoRepository.upsertPromo(packageId, visible, displayOrder);
        return packagePromoRepository.findByPackageId(packageId).orElseThrow();
    }

    /** Retire le package de la promo : il redevient un package Omra ordinaire. */
    public void removePromoPackage(long packageId) {
        if (!packagePromoRepository.deletePromo(packageId)) {
            throw new PromoPackageException.PromoPackageNotFoundException(
                    "Ce package n'est pas dans l'offre (id=" + packageId + ")");
        }
    }
}
