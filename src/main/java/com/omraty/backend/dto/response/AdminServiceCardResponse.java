package com.omraty.backend.dto.response;

/**
 * Carte de service côté admin (POST/PATCH /home/service-cards) : renvoie les 3 variantes de
 * title/description/buttonText (au lieu des champs uniques résolus de {@link ServiceCardResponse},
 * destinés à l'app) pour permettre l'édition complète de chaque langue depuis l'admin.
 */
public record AdminServiceCardResponse(
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
        boolean comingSoon,
        boolean visible) {}
