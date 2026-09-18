package com.omraty.backend.service;

import com.omraty.backend.entities.Banner;
import com.omraty.backend.exception.BannerException;
import com.omraty.backend.repository.BannerRepository;
import com.omraty.backend.storage.FileStorageService;
import com.omraty.backend.storage.PublicUrlResolver;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BannerService {

    private static final String BANNER_IMAGE_SUBDIR = "banner";
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png");
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;
    private final PublicUrlResolver publicUrlResolver;

    public BannerService(
            BannerRepository bannerRepository,
            FileStorageService fileStorageService,
            PublicUrlResolver publicUrlResolver) {
        this.bannerRepository = bannerRepository;
        this.fileStorageService = fileStorageService;
        this.publicUrlResolver = publicUrlResolver;
    }

    /** Bannières visibles, triées par ordre d'affichage, telles que retournées à l'app. */
    public List<Banner> getActiveBanners() {
        return bannerRepository.findActiveBanners();
    }

    /** Toutes les bannières (visibles ou masquées), pour l'écran d'administration. */
    public List<Banner> getAllBanners() {
        return bannerRepository.findAllBanners();
    }

    /**
     * Ajoute une nouvelle bannière avec son image. displayOrder non fourni = ajoutée en fin de
     * liste ; visible non fourni = visible par défaut.
     */
    public Banner createBanner(
            MultipartFile image,
            String title,
            String description,
            Integer displayOrder,
            Boolean visible) {
        validateImage(image);
        validateText(title, MAX_TITLE_LENGTH, "Le titre");
        validateText(description, MAX_DESCRIPTION_LENGTH, "La description");
        String storedKey = fileStorageService.store(image, BANNER_IMAGE_SUBDIR);
        String imageUrl = publicUrlResolver.toPublicUrl(storedKey);
        int order = displayOrder != null ? displayOrder : bannerRepository.nextDisplayOrder();
        boolean isVisible = visible == null || visible;
        return bannerRepository.insert(imageUrl, title, description, order, isVisible);
    }

    /**
     * Met à jour une bannière existante (titre, description, ordre, visibilité). Tous les champs
     * sont optionnels : seuls ceux fournis (non null) sont modifiés. L'image se change via {@link
     * #updateImage(long, MultipartFile)}.
     */
    public Banner updateBanner(
            long id, String title, String description, Integer displayOrder, Boolean visible) {
        validateText(title, MAX_TITLE_LENGTH, "Le titre");
        validateText(description, MAX_DESCRIPTION_LENGTH, "La description");
        return bannerRepository
                .update(id, title, description, displayOrder, visible)
                .orElseThrow(
                        () ->
                                new BannerException.BannerNotFoundException(
                                        "Bannière introuvable (id=" + id + ")"));
    }

    /** Change l'image d'une bannière existante, sans toucher à ses autres champs. */
    public Banner updateImage(long id, MultipartFile image) {
        validateImage(image);
        String storedKey = fileStorageService.store(image, BANNER_IMAGE_SUBDIR);
        String imageUrl = publicUrlResolver.toPublicUrl(storedKey);
        return bannerRepository
                .updateImage(id, imageUrl)
                .orElseThrow(
                        () ->
                                new BannerException.BannerNotFoundException(
                                        "Bannière introuvable (id=" + id + ")"));
    }

    /** Supprime une bannière. */
    public void deleteBanner(long id) {
        if (!bannerRepository.deleteById(id)) {
            throw new BannerException.BannerNotFoundException(
                    "Bannière introuvable (id=" + id + ")");
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BannerException.InvalidBannerRequestException(
                    "L'image de la bannière est requise");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BannerException.InvalidBannerRequestException(
                    "L'image dépasse la taille maximale autorisée (5 Mo)");
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BannerException.InvalidBannerRequestException(
                    "Format d'image non supporté (JPEG ou PNG uniquement)");
        }
    }

    private void validateText(String value, int maxLength, String fieldLabel) {
        if (value != null && value.length() > maxLength) {
            throw new BannerException.InvalidBannerRequestException(
                    fieldLabel + " dépasse la longueur maximale autorisée (" + maxLength + ")");
        }
    }
}
