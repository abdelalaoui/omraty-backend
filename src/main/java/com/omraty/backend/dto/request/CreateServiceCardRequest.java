package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * description et imageUrl sont optionnels (imageUrl peut aussi être réglé par upload, voir PATCH
 * /home/service-cards/{id}/image) ; comingSoon/visible sont optionnels : par défaut la carte est
 * créée visible et pas en mode "coming soon".
 */
public record CreateServiceCardRequest(
        @NotBlank(message = "Le type est requis") String type,
        @NotBlank(message = "Le titre est requis") String title,
        String description,
        @NotBlank(message = "Le texte du bouton est requis") String buttonText,
        @NotBlank(message = "L'icône est requise") String icon,
        String imageUrl,
        Boolean comingSoon,
        Boolean visible) {}
