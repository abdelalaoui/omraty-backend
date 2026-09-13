package com.omraty.backend.service;

import com.omraty.backend.entities.ServiceCard;
import com.omraty.backend.exception.ServiceCardException;
import com.omraty.backend.repository.ServiceCardRepository;
import com.omraty.backend.storage.FileStorageService;
import com.omraty.backend.storage.PublicUrlResolver;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServiceCardService {

    private static final int MAX_TYPE_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_BUTTON_TEXT_LENGTH = 100;
    private static final int MAX_ICON_LENGTH = 100;
    private static final int MAX_IMAGE_URL_LENGTH = 500;

    private static final String SERVICE_CARD_IMAGE_SUBDIR = "service-card";
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png");

    private final ServiceCardRepository serviceCardRepository;
    private final FileStorageService fileStorageService;
    private final PublicUrlResolver publicUrlResolver;

    public ServiceCardService(
            ServiceCardRepository serviceCardRepository,
            FileStorageService fileStorageService,
            PublicUrlResolver publicUrlResolver) {
        this.serviceCardRepository = serviceCardRepository;
        this.fileStorageService = fileStorageService;
        this.publicUrlResolver = publicUrlResolver;
    }

    /** Cartes actives à afficher sur la home, avec leur contenu et leur état (comingSoon). */
    public List<ServiceCard> getActiveServiceCards() {
        return serviceCardRepository.findActiveServiceCards();
    }

    /**
     * Ajoute une nouvelle carte de service. Seuls titleFr et buttonTextFr sont requis :
     * titleEn/titleAr et buttonTextEn/buttonTextAr sont optionnels (repli sur le _fr tant qu'ils ne
     * sont pas traduits, voir ServiceCardMapper), descriptionFr/En/Ar restent optionnels comme
     * l'était l'ancienne description unique. imageUrl est optionnelle (réglable ici en texte, ou
     * plus tard par upload, voir {@link #updateImage}) ; comingSoon/visible non fournis = valeurs
     * par défaut (pas en mode coming soon, visible) — extensible à un nouveau type de service sans
     * changement de code.
     */
    public ServiceCard createServiceCard(
            String type,
            String titleFr,
            String titleEn,
            String titleAr,
            String descriptionFr,
            String descriptionEn,
            String descriptionAr,
            String buttonTextFr,
            String buttonTextEn,
            String buttonTextAr,
            String icon,
            String imageUrl,
            Boolean comingSoon,
            Boolean visible) {
        validateType(type);
        validateTitle(titleFr);
        validateOptionalTranslation(titleEn, "titleEn", MAX_TITLE_LENGTH);
        validateOptionalTranslation(titleAr, "titleAr", MAX_TITLE_LENGTH);
        validateDescription(descriptionFr);
        validateDescription(descriptionEn);
        validateDescription(descriptionAr);
        validateButtonText(buttonTextFr);
        validateOptionalTranslation(buttonTextEn, "buttonTextEn", MAX_BUTTON_TEXT_LENGTH);
        validateOptionalTranslation(buttonTextAr, "buttonTextAr", MAX_BUTTON_TEXT_LENGTH);
        validateIcon(icon);
        validateImageUrl(imageUrl);
        boolean isComingSoon = comingSoon != null && comingSoon;
        boolean isVisible = visible == null || visible;
        return serviceCardRepository.insert(
                type,
                titleFr,
                titleEn,
                titleAr,
                descriptionFr,
                descriptionEn,
                descriptionAr,
                buttonTextFr,
                buttonTextEn,
                buttonTextAr,
                icon,
                imageUrl,
                isComingSoon,
                isVisible);
    }

    /**
     * Met à jour une carte existante. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. basculer comingSoon (mode désactivé/popup) sans toucher au reste du
     * contenu.
     */
    public ServiceCard updateServiceCard(
            long id,
            String type,
            String titleFr,
            String titleEn,
            String titleAr,
            String descriptionFr,
            String descriptionEn,
            String descriptionAr,
            String buttonTextFr,
            String buttonTextEn,
            String buttonTextAr,
            String icon,
            String imageUrl,
            Boolean comingSoon,
            Boolean visible) {
        if (type != null) {
            validateType(type);
        }
        if (titleFr != null) {
            validateTitle(titleFr);
        }
        validateOptionalTranslation(titleEn, "titleEn", MAX_TITLE_LENGTH);
        validateOptionalTranslation(titleAr, "titleAr", MAX_TITLE_LENGTH);
        validateDescription(descriptionFr);
        validateDescription(descriptionEn);
        validateDescription(descriptionAr);
        if (buttonTextFr != null) {
            validateButtonText(buttonTextFr);
        }
        validateOptionalTranslation(buttonTextEn, "buttonTextEn", MAX_BUTTON_TEXT_LENGTH);
        validateOptionalTranslation(buttonTextAr, "buttonTextAr", MAX_BUTTON_TEXT_LENGTH);
        if (icon != null) {
            validateIcon(icon);
        }
        if (imageUrl != null) {
            validateImageUrl(imageUrl);
        }
        return serviceCardRepository
                .update(
                        id,
                        type,
                        titleFr,
                        titleEn,
                        titleAr,
                        descriptionFr,
                        descriptionEn,
                        descriptionAr,
                        buttonTextFr,
                        buttonTextEn,
                        buttonTextAr,
                        icon,
                        imageUrl,
                        comingSoon,
                        visible)
                .orElseThrow(
                        () ->
                                new ServiceCardException.ServiceCardNotFoundException(
                                        "Carte de service introuvable (id=" + id + ")"));
    }

    /**
     * Upload admin de l'image affichée dans le cercle de la carte (même mécanisme S3/local que la
     * bannière, voir BannerService.updateImage) : ne touche qu'à imageUrl, le reste du contenu
     * n'est pas modifié.
     */
    public ServiceCard updateImage(long id, MultipartFile image) {
        validateImage(image);
        String storedKey = fileStorageService.store(image, SERVICE_CARD_IMAGE_SUBDIR);
        String imageUrl = publicUrlResolver.toPublicUrl(storedKey);
        return serviceCardRepository
                .updateImage(id, imageUrl)
                .orElseThrow(
                        () ->
                                new ServiceCardException.ServiceCardNotFoundException(
                                        "Carte de service introuvable (id=" + id + ")"));
    }

    private void validateType(String type) {
        if (type.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le type ne peut pas être vide");
        }
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le type dépasse la longueur maximale autorisée (" + MAX_TYPE_LENGTH + ")");
        }
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le titre ne peut pas être vide");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le titre dépasse la longueur maximale autorisée (" + MAX_TITLE_LENGTH + ")");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "La description dépasse la longueur maximale autorisée ("
                            + MAX_DESCRIPTION_LENGTH
                            + ")");
        }
    }

    private void validateButtonText(String buttonText) {
        if (buttonText.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le texte du bouton ne peut pas être vide");
        }
        if (buttonText.length() > MAX_BUTTON_TEXT_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le texte du bouton dépasse la longueur maximale autorisée ("
                            + MAX_BUTTON_TEXT_LENGTH
                            + ")");
        }
    }

    /**
     * Variantes en/ar de title et buttonText : optionnelles (repli sur le _fr tant qu'absentes,
     * voir ServiceCardMapper), mais si fournies elles suivent les mêmes règles que leur champ
     * requis (non vide, longueur maximale) pour éviter une traduction blanche ou tronquée
     * silencieuse.
     */
    private void validateOptionalTranslation(String value, String fieldName, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le champ " + fieldName + " ne peut pas être vide s'il est fourni");
        }
        if (value.length() > maxLength) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + maxLength
                            + ")");
        }
    }

    private void validateIcon(String icon) {
        if (icon.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'icône ne peut pas être vide");
        }
        if (icon.length() > MAX_ICON_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'icône dépasse la longueur maximale autorisée (" + MAX_ICON_LENGTH + ")");
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.length() > MAX_IMAGE_URL_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'URL de l'image dépasse la longueur maximale autorisée ("
                            + MAX_IMAGE_URL_LENGTH
                            + ")");
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'image de la carte est requise");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'image dépasse la taille maximale autorisée (5 Mo)");
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Format d'image non supporté (JPEG ou PNG uniquement)");
        }
    }
}
