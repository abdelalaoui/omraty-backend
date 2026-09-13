package com.omraty.backend.entities;

import java.time.LocalDateTime;

/**
 * titleFr/titleEn/titleAr, descriptionFr/descriptionEn/descriptionAr et
 * buttonTextFr/buttonTextEn/buttonTextAr : une colonne par langue plutôt qu'une table de
 * traduction, vu le nombre fixe et restreint de cartes (Omra/Hajj/Visa à date) — GET
 * /home/service-cards choisit celle du header Accept-Language (voir ServiceCardController) et la
 * renvoie sous les clés title/description/buttonText existantes. Les variantes en/ar (et
 * description, déjà optionnelle) sont nullables, avec repli sur la variante fr si absentes (voir
 * ServiceCardMapper) — laissées à traduire manuellement depuis l'admin plutôt que devinées.
 */
public record ServiceCard(
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
        boolean visible,
        LocalDateTime updatedAt) {}
