package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * titleFr et buttonTextFr sont requis ; titleEn/titleAr et buttonTextEn/buttonTextAr sont
 * optionnels (repli sur le _fr tant qu'ils ne sont pas traduits, voir ServiceCardMapper).
 * descriptionFr/descriptionEn/descriptionAr et imageUrl sont optionnels (imageUrl peut aussi être
 * réglé par upload, voir PATCH /home/service-cards/{id}/image) ; comingSoon/visible sont optionnels
 * : par défaut la carte est créée visible et pas en mode "coming soon".
 */
public record CreateServiceCardRequest(
        @NotBlank(message = "Le type est requis") String type,
        @NotBlank(message = "Le titre en français est requis") String titleFr,
        String titleEn,
        String titleAr,
        String descriptionFr,
        String descriptionEn,
        String descriptionAr,
        @NotBlank(message = "Le texte du bouton en français est requis") String buttonTextFr,
        String buttonTextEn,
        String buttonTextAr,
        @NotBlank(message = "L'icône est requise") String icon,
        String imageUrl,
        Boolean comingSoon,
        Boolean visible) {}
