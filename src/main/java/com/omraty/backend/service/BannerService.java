package com.omraty.backend.service;

import com.omraty.backend.entities.Banner;
import com.omraty.backend.exception.BannerException;
import com.omraty.backend.repository.BannerRepository;
import com.omraty.backend.storage.FileStorageService;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String S3_PROVIDER = "s3";

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;
    private final String uploadProvider;
    private final String s3Bucket;
    private final String s3Region;

    public BannerService(
            BannerRepository bannerRepository,
            FileStorageService fileStorageService,
            @Value("${app.upload.provider}") String uploadProvider,
            @Value("${app.upload.s3.bucket}") String s3Bucket,
            @Value("${app.upload.s3.region}") String s3Region) {
        this.bannerRepository = bannerRepository;
        this.fileStorageService = fileStorageService;
        this.uploadProvider = uploadProvider;
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
    }

    /** Bannière courante, telle que retournée à l'app pour l'écran d'accueil. */
    public Banner getBanner() {
        return bannerRepository
                .findBanner()
                .orElseThrow(
                        () -> new BannerException.BannerNotFoundException("Bannière introuvable"));
    }

    /**
     * Change l'image de la bannière, sans toucher à sa visibilité. title/description sont
     * optionnels : une image seule suffit, ou l'image accompagnée d'un titre et/ou d'une
     * description (non fournis = valeurs existantes conservées).
     */
    public Banner updateImage(MultipartFile image, String title, String description) {
        validateImage(image);
        validateText(title, MAX_TITLE_LENGTH, "Le titre");
        validateText(description, MAX_DESCRIPTION_LENGTH, "La description");
        String storedKey = fileStorageService.store(image, BANNER_IMAGE_SUBDIR);
        String imageUrl = toPublicUrl(storedKey);
        return bannerRepository
                .updateImage(imageUrl, title, description)
                .orElseThrow(
                        () -> new BannerException.BannerNotFoundException("Bannière introuvable"));
    }

    /**
     * En stockage S3, {@link FileStorageService#store} ne renvoie que la clé de l'objet (le bucket
     * n'est pas public par défaut). Le dossier banner/ est configuré public en lecture côté infra,
     * donc on reconstruit ici l'URL publique complète à partir de la clé. En stockage local, la clé
     * est déjà une URL utilisable telle quelle.
     */
    private String toPublicUrl(String storedKey) {
        if (!S3_PROVIDER.equalsIgnoreCase(uploadProvider)) {
            return storedKey;
        }
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + storedKey;
    }

    /** Affiche/masque la bannière sur l'écran d'accueil sans supprimer ses données. */
    public Banner updateVisibility(boolean visible) {
        return bannerRepository
                .updateVisibility(visible)
                .orElseThrow(
                        () -> new BannerException.BannerNotFoundException("Bannière introuvable"));
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
