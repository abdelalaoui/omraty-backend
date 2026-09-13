package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminServiceCardResponse;
import com.omraty.backend.dto.response.ServiceCardResponse;
import com.omraty.backend.entities.ServiceCard;
import java.util.List;

public final class ServiceCardMapper {

    private static final String DEFAULT_LANGUAGE = "ar";

    private ServiceCardMapper() {}

    /**
     * acceptLanguage : header brut de la requête (fr|en|ar), ar par défaut si absent ou non reconnu
     * — voir ServiceCardController.
     */
    public static List<ServiceCardResponse> toResponseList(
            List<ServiceCard> serviceCards, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return serviceCards.stream().map(card -> toResponse(card, language)).toList();
    }

    public static ServiceCardResponse toResponse(ServiceCard serviceCard, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return new ServiceCardResponse(
                serviceCard.id(),
                serviceCard.type(),
                titleFor(serviceCard, language),
                descriptionFor(serviceCard, language),
                buttonTextFor(serviceCard, language),
                serviceCard.icon(),
                serviceCard.imageUrl(),
                serviceCard.comingSoon(),
                serviceCard.visible());
    }

    public static AdminServiceCardResponse toAdminResponse(ServiceCard serviceCard) {
        return new AdminServiceCardResponse(
                serviceCard.id(),
                serviceCard.type(),
                serviceCard.titleFr(),
                serviceCard.titleEn(),
                serviceCard.titleAr(),
                serviceCard.descriptionFr(),
                serviceCard.descriptionEn(),
                serviceCard.descriptionAr(),
                serviceCard.buttonTextFr(),
                serviceCard.buttonTextEn(),
                serviceCard.buttonTextAr(),
                serviceCard.icon(),
                serviceCard.imageUrl(),
                serviceCard.comingSoon(),
                serviceCard.visible());
    }

    // titleEn/titleAr et buttonTextEn/buttonTextAr peuvent ne pas encore être traduits : repli sur
    // le _fr (toujours renseigné) plutôt que de renvoyer un champ vide à l'app.
    private static String titleFor(ServiceCard serviceCard, String language) {
        String translated =
                switch (language) {
                    case "en" -> serviceCard.titleEn();
                    case "ar" -> serviceCard.titleAr();
                    default -> serviceCard.titleFr();
                };
        return translated != null ? translated : serviceCard.titleFr();
    }

    private static String descriptionFor(ServiceCard serviceCard, String language) {
        String translated =
                switch (language) {
                    case "en" -> serviceCard.descriptionEn();
                    case "ar" -> serviceCard.descriptionAr();
                    default -> serviceCard.descriptionFr();
                };
        return translated != null ? translated : serviceCard.descriptionFr();
    }

    private static String buttonTextFor(ServiceCard serviceCard, String language) {
        String translated =
                switch (language) {
                    case "en" -> serviceCard.buttonTextEn();
                    case "ar" -> serviceCard.buttonTextAr();
                    default -> serviceCard.buttonTextFr();
                };
        return translated != null ? translated : serviceCard.buttonTextFr();
    }

    private static String resolveLanguage(String acceptLanguage) {
        if (acceptLanguage == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = acceptLanguage.trim().toLowerCase();
        return switch (normalized) {
            case "fr", "en", "ar" -> normalized;
            default -> DEFAULT_LANGUAGE;
        };
    }
}
