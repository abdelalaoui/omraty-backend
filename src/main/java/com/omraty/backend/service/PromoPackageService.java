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
 * Packages promo de la bannière BED_OFFER (voir migration V51) : des packages Omra ordinaires —
 * mêmes dates, chambres, lits, paiements, achats et factures — à deux différences près :
 *
 * <ul>
 *   <li>ils ne sont proposés que depuis la bannière ({@link #getBookablePackages}) et ne se
 *       réservent que par RoomService.reserveOfferBed : le parcours Omra normal les ignore (voir
 *       PackageService.getReservationGroups) et les refuse ({@link #ensureNotPromo}) ;
 *   <li>chacun a son propre prix de lit ({@link #resolveBedPrice}) au lieu du tarif global des
 *       formules.
 * </ul>
 *
 * Le réglage bed_offer_enabled (V47) reste l'interrupteur général de l'offre ; bed_offer_price
 * (prix unique) n'est plus lu.
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

    /** Packages proposés depuis la bannière (visibles, avec un prix) ; vide si offre coupée. */
    public List<PromoPackage> getBookablePackages() {
        return isOfferEnabled() ? packagePromoRepository.findBookable() : List.of();
    }

    /**
     * Prix de lit le plus bas des packages proposés, pour le ctaPrice de la bannière (voir
     * BannerController) : les versions de l'app antérieures aux packages promo n'ouvrent l'offre
     * que si ce prix est renseigné. Vide si l'offre est coupée ou sans package proposé.
     */
    public Optional<BigDecimal> getMinimumBedPrice() {
        return isOfferEnabled()
                ? packagePromoRepository.findMinBookableBedPrice()
                : Optional.empty();
    }

    /**
     * Prix réellement facturé pour un lit de ce package via l'offre (voir
     * RoomService.reserveOfferBed).
     *
     * @throws BookingPaymentException.PriceNotConfiguredException si l'offre est coupée.
     * @throws PromoPackageException.PackageNotInOfferException si le package n'est pas proposé
     *     dans l'offre (pas en promo, masqué ou sans prix).
     */
    public BigDecimal resolveBedPrice(long packageId) {
        if (!isOfferEnabled()) {
            throw new BookingPaymentException.PriceNotConfiguredException(
                    "L'offre spéciale n'est pas active actuellement");
        }
        return packagePromoRepository
                .findByPackageId(packageId)
                .filter(PromoPackage::visible)
                .map(PromoPackage::bedPrice)
                .orElseThrow(
                        () ->
                                new PromoPackageException.PackageNotInOfferException(
                                        "Ce package ne fait pas partie de l'offre"));
    }

    /**
     * Les packages promo ne se réservent que depuis la bannière (prix propre, paiement complet) :
     * refuse les routes de réservation du parcours Omra normal.
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
     * Met un package Omra existant en promo, ou modifie sa promo : bedPrice requis à l'ajout,
     * visible/displayOrder optionnels (valeur existante, ou visible et en tête à l'ajout).
     */
    @Transactional
    public PromoPackage upsertPromoPackage(
            long packageId, BigDecimal bedPrice, Boolean visible, Integer displayOrder) {
        packageRepository
                .findById(packageId)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + packageId + ")"));
        boolean alreadyPromo = packagePromoRepository.isPromo(packageId);
        if (bedPrice == null && !alreadyPromo) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "Le prix du lit est requis pour ajouter un package à l'offre");
        }
        if (bedPrice != null && bedPrice.signum() <= 0) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "Le prix du lit doit être supérieur à 0");
        }
        packagePromoRepository.upsertPromo(packageId, visible, displayOrder);
        if (bedPrice != null) {
            packagePromoRepository.upsertBedPrice(packageId, bedPrice);
        }
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
