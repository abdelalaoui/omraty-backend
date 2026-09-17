package com.omraty.backend.service;

import com.omraty.backend.entities.TripPackage;
import com.omraty.backend.entities.enums.TripPackageCategory;
import com.omraty.backend.exception.TripPackageException;
import com.omraty.backend.repository.TripPackageImageRepository;
import com.omraty.backend.repository.TripPackageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue de voyages Omra (CatalogScreen côté app, voir PackageModel) : fiches produit avec prix,
 * destination, catégorie, dates et images — à ne pas confondre avec OmraPackage (regroupement de
 * pèlerins pour les réservations de chambres, voir PackageService/AdminPackageController), un
 * concept distinct. Lecture publique limitée aux packages visibles (voir {@link
 * #getVisiblePackages} et {@link #getVisiblePackageById}) ; gestion réservée à ROLE_ADMIN (voir
 * SecurityConfig, préfixe /admin/**, et AdminTripPackageController).
 */
@Service
public class TripPackageService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESTINATION_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_IMAGE_URL_LENGTH = 500;

    private final TripPackageRepository tripPackageRepository;
    private final TripPackageImageRepository tripPackageImageRepository;

    public TripPackageService(
            TripPackageRepository tripPackageRepository,
            TripPackageImageRepository tripPackageImageRepository) {
        this.tripPackageRepository = tripPackageRepository;
        this.tripPackageImageRepository = tripPackageImageRepository;
    }

    /**
     * Packages visibles du catalogue, filtrés (tous les filtres sont optionnels) : destination
     * (contient, insensible à la casse), category, et le prix entre minBudget et maxBudget —
     * contrat exact envoyé par l'app (voir CatalogScreen).
     */
    public List<TripPackageWithImages> getVisiblePackages(
            String destination,
            TripPackageCategory category,
            BigDecimal minBudget,
            BigDecimal maxBudget) {
        validateBudgetRange(minBudget, maxBudget);
        List<TripPackage> packages =
                tripPackageRepository.findVisibleFiltered(
                        destination, category, minBudget, maxBudget);
        Map<Long, List<String>> imagesByPackageId =
                tripPackageImageRepository.findUrlsByPackageIds(
                        packages.stream().map(TripPackage::id).toList());
        return packages.stream()
                .map(
                        pkg ->
                                new TripPackageWithImages(
                                        pkg, imagesByPackageId.getOrDefault(pkg.id(), List.of())))
                .toList();
    }

    /** Détail d'un package visible du catalogue (les packages masqués ne sont pas exposés). */
    public TripPackageWithImages getVisiblePackageById(long id) {
        TripPackage pkg =
                tripPackageRepository
                        .findById(id)
                        .filter(TripPackage::visible)
                        .orElseThrow(
                                () ->
                                        new TripPackageException.TripPackageNotFoundException(
                                                "Package introuvable (id=" + id + ")"));
        return new TripPackageWithImages(pkg, tripPackageImageRepository.findUrlsByPackageId(id));
    }

    /**
     * Ajoute un nouveau package au catalogue. visible non fourni = visible par défaut ; imageUrls
     * non fourni = pas d'image pour l'instant, ajoutables plus tard via PATCH.
     */
    @Transactional
    public TripPackageWithImages createPackage(
            String title,
            String destination,
            TripPackageCategory category,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            Boolean includesVisa,
            Integer groupSize,
            Boolean visible,
            List<String> imageUrls) {
        validateTitle(title);
        validateDestination(destination);
        validatePrice(price);
        validateDates(startDate, endDate);
        validateDescription(description);
        validateGroupSize(groupSize);
        List<String> resolvedImageUrls = imageUrls == null ? List.of() : imageUrls;
        validateImageUrls(resolvedImageUrls);
        boolean isVisible = visible == null || visible;
        TripPackage created =
                tripPackageRepository.insert(
                        title,
                        destination,
                        category,
                        price,
                        startDate,
                        endDate,
                        description,
                        includesVisa,
                        groupSize,
                        isVisible);
        tripPackageImageRepository.replaceImages(created.id(), resolvedImageUrls);
        return new TripPackageWithImages(created, resolvedImageUrls);
    }

    /**
     * Met à jour un package existant. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. ne changer que le prix, ou masquer un package (visible) sans
     * toucher au reste. imageUrls non fourni (null) laisse les images existantes inchangées ;
     * fourni (y compris liste vide), il remplace entièrement la galerie.
     */
    @Transactional
    public TripPackageWithImages updatePackage(
            long id,
            String title,
            String destination,
            TripPackageCategory category,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            Boolean includesVisa,
            Integer groupSize,
            Boolean visible,
            List<String> imageUrls) {
        if (title != null) {
            validateTitle(title);
        }
        if (destination != null) {
            validateDestination(destination);
        }
        if (price != null) {
            validatePrice(price);
        }
        if (startDate != null || endDate != null) {
            TripPackage existing = getPackageOrThrow(id);
            LocalDate effectiveStartDate = startDate != null ? startDate : existing.startDate();
            LocalDate effectiveEndDate = endDate != null ? endDate : existing.endDate();
            validateDates(effectiveStartDate, effectiveEndDate);
        }
        validateDescription(description);
        validateGroupSize(groupSize);
        if (imageUrls != null) {
            validateImageUrls(imageUrls);
        }
        TripPackage updated =
                tripPackageRepository
                        .update(
                                id,
                                title,
                                destination,
                                category,
                                price,
                                startDate,
                                endDate,
                                description,
                                includesVisa,
                                groupSize,
                                visible)
                        .orElseThrow(
                                () ->
                                        new TripPackageException.TripPackageNotFoundException(
                                                "Package introuvable (id=" + id + ")"));
        if (imageUrls != null) {
            tripPackageImageRepository.replaceImages(id, imageUrls);
        }
        return new TripPackageWithImages(
                updated, tripPackageImageRepository.findUrlsByPackageId(id));
    }

    private TripPackage getPackageOrThrow(long id) {
        return tripPackageRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new TripPackageException.TripPackageNotFoundException(
                                        "Package introuvable (id=" + id + ")"));
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "Le titre ne peut pas être vide");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "Le titre dépasse la longueur maximale autorisée (" + MAX_TITLE_LENGTH + ")");
        }
    }

    private void validateDestination(String destination) {
        if (destination.isBlank()) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "La destination ne peut pas être vide");
        }
        if (destination.length() > MAX_DESTINATION_LENGTH) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "La destination dépasse la longueur maximale autorisée ("
                            + MAX_DESTINATION_LENGTH
                            + ")");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "Le prix doit être positif");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "La date de retour ne peut pas précéder la date de départ");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "La description dépasse la longueur maximale autorisée ("
                            + MAX_DESCRIPTION_LENGTH
                            + ")");
        }
    }

    private void validateGroupSize(Integer groupSize) {
        if (groupSize != null && groupSize <= 0) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "La taille du groupe doit être positive");
        }
    }

    private void validateImageUrls(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new TripPackageException.InvalidTripPackageRequestException(
                        "Une URL d'image ne peut pas être vide");
            }
            if (imageUrl.length() > MAX_IMAGE_URL_LENGTH) {
                throw new TripPackageException.InvalidTripPackageRequestException(
                        "Une URL d'image dépasse la longueur maximale autorisée ("
                                + MAX_IMAGE_URL_LENGTH
                                + ")");
            }
        }
    }

    private void validateBudgetRange(BigDecimal minBudget, BigDecimal maxBudget) {
        if (minBudget != null && maxBudget != null && minBudget.compareTo(maxBudget) > 0) {
            throw new TripPackageException.InvalidTripPackageRequestException(
                    "minBudget ne peut pas dépasser maxBudget");
        }
    }
}
