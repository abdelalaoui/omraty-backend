package com.omraty.backend.service;

import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.repository.PromoPackageRepository;
import com.omraty.backend.repository.PromoPackageTierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Gestion admin des packages promo accessibles depuis la bannière de l'app (voir Banner) :
 * indépendants du catalogue normal ({@link TripPackageService}), avec leurs propres infos et prix
 * par type de chambre (2/3/5, voir PromoPackageTier). Réservé à ROLE_ADMIN (voir SecurityConfig,
 * préfixe /admin/**, et AdminPromoPackageController). Un package promo sans tier n'est pas exposé
 * côté app (filtrage à la charge de l'endpoint public de lecture, hors scope ici).
 */
@Service
public class PromoPackageService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Set<Integer> VALID_TIER_TYPES = Set.of(2, 3, 5);

    private final PromoPackageRepository promoPackageRepository;
    private final PromoPackageTierRepository promoPackageTierRepository;

    public PromoPackageService(
            PromoPackageRepository promoPackageRepository,
            PromoPackageTierRepository promoPackageTierRepository) {
        this.promoPackageRepository = promoPackageRepository;
        this.promoPackageTierRepository = promoPackageTierRepository;
    }

    /** Tous les packages promo, avec ou sans tier (vue admin, non filtrée). */
    public List<PromoPackageWithTiers> getPromoPackages() {
        List<PromoPackage> packages = promoPackageRepository.findAll();
        var tiersByPackageId =
                promoPackageTierRepository.findByPromoPackageIds(
                        packages.stream().map(PromoPackage::id).toList());
        return packages.stream()
                .map(
                        pkg ->
                                new PromoPackageWithTiers(
                                        pkg, tiersByPackageId.getOrDefault(pkg.id(), List.of())))
                .toList();
    }

    public PromoPackageWithTiers getPromoPackageById(long id) {
        PromoPackage pkg = getPromoPackageOrThrow(id);
        return new PromoPackageWithTiers(pkg, promoPackageTierRepository.findByPromoPackageId(id));
    }

    /** Ajoute un nouveau package promo, sans tier (ajoutés ensuite via {@link #addTier}). */
    public PromoPackageWithTiers createPromoPackage(String title, String description) {
        validateTitle(title);
        validateDescription(description);
        PromoPackage created = promoPackageRepository.insert(title, description);
        return new PromoPackageWithTiers(created, List.of());
    }

    /**
     * Met à jour un package promo existant. Tous les champs sont optionnels : seuls ceux fournis
     * (non null) sont modifiés.
     */
    public PromoPackageWithTiers updatePromoPackage(long id, String title, String description) {
        if (title != null) {
            validateTitle(title);
        }
        validateDescription(description);
        PromoPackage updated =
                promoPackageRepository
                        .update(id, title, description)
                        .orElseThrow(() -> notFound(id));
        return new PromoPackageWithTiers(
                updated, promoPackageTierRepository.findByPromoPackageId(id));
    }

    /** Supprime un package promo (et ses tiers, en cascade — voir migration V42). */
    public void deletePromoPackage(long id) {
        if (!promoPackageRepository.deleteById(id)) {
            throw notFound(id);
        }
    }

    /**
     * Ajoute un tier de prix à un package promo. Un même type (2, 3 ou 5) ne peut apparaître qu'une
     * fois par package (voir contrainte unique, migration V42).
     */
    public PromoPackageTier addTier(
            long promoPackageId, Integer type, Integer capacity, BigDecimal price) {
        getPromoPackageOrThrow(promoPackageId);
        validateTierType(type);
        validateCapacity(capacity);
        validatePrice(price);
        List<PromoPackageTier> existingTiers =
                promoPackageTierRepository.findByPromoPackageId(promoPackageId);
        ensureNoDuplicateType(existingTiers, type, null);
        return promoPackageTierRepository.insert(promoPackageId, type, capacity, price);
    }

    /**
     * Met à jour un tier existant. Tous les champs sont optionnels : seuls ceux fournis (non null)
     * sont modifiés.
     */
    public PromoPackageTier updateTier(
            long promoPackageId, long tierId, Integer type, Integer capacity, BigDecimal price) {
        getPromoPackageOrThrow(promoPackageId);
        PromoPackageTier existing = getTierOrThrow(promoPackageId, tierId);
        if (type != null) {
            validateTierType(type);
            List<PromoPackageTier> siblingTiers =
                    promoPackageTierRepository.findByPromoPackageId(promoPackageId);
            ensureNoDuplicateType(siblingTiers, type, tierId);
        }
        if (capacity != null) {
            validateCapacity(capacity);
        }
        if (price != null) {
            validatePrice(price);
        }
        return promoPackageTierRepository
                .update(tierId, type, capacity, price)
                .orElseThrow(
                        () ->
                                new PromoPackageException.PromoPackageTierNotFoundException(
                                        "Tier introuvable (id=" + tierId + ")"));
    }

    public void deleteTier(long promoPackageId, long tierId) {
        getPromoPackageOrThrow(promoPackageId);
        getTierOrThrow(promoPackageId, tierId);
        promoPackageTierRepository.deleteById(tierId);
    }

    private PromoPackage getPromoPackageOrThrow(long id) {
        return promoPackageRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private PromoPackageTier getTierOrThrow(long promoPackageId, long tierId) {
        PromoPackageTier tier =
                promoPackageTierRepository
                        .findById(tierId)
                        .orElseThrow(
                                () ->
                                        new PromoPackageException.PromoPackageTierNotFoundException(
                                                "Tier introuvable (id=" + tierId + ")"));
        if (tier.promoPackageId() != promoPackageId) {
            throw new PromoPackageException.PromoPackageTierNotFoundException(
                    "Tier introuvable (id="
                            + tierId
                            + ") pour ce package (id="
                            + promoPackageId
                            + ")");
        }
        return tier;
    }

    private void ensureNoDuplicateType(
            List<PromoPackageTier> existingTiers, int type, Long excludeTierId) {
        boolean duplicate =
                existingTiers.stream()
                        .anyMatch(
                                tier ->
                                        tier.type() == type
                                                && (excludeTierId == null
                                                        || tier.id() != excludeTierId));
        if (duplicate) {
            throw new PromoPackageException.InvalidPromoPackageTierRequestException(
                    "Ce package a déjà un tier de type " + type);
        }
    }

    private PromoPackageException.PromoPackageNotFoundException notFound(long id) {
        return new PromoPackageException.PromoPackageNotFoundException(
                "Package promo introuvable (id=" + id + ")");
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "Le titre ne peut pas être vide");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "Le titre dépasse la longueur maximale autorisée (" + MAX_TITLE_LENGTH + ")");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "La description dépasse la longueur maximale autorisée ("
                            + MAX_DESCRIPTION_LENGTH
                            + ")");
        }
    }

    private void validateTierType(Integer type) {
        if (type == null || !VALID_TIER_TYPES.contains(type)) {
            throw new PromoPackageException.InvalidPromoPackageTierRequestException(
                    "Le type doit être 2, 3 ou 5");
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new PromoPackageException.InvalidPromoPackageTierRequestException(
                    "La capacité doit être positive");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PromoPackageException.InvalidPromoPackageTierRequestException(
                    "Le prix doit être positif");
        }
    }
}
