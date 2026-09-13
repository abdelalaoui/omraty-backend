package com.omraty.backend.dto.request;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne basculer que comingSoon
 * (mode désactivé/popup) ou visible (masquer la carte) sans toucher au reste du contenu, ou de ne
 * traduire que titleEn sans toucher à titleFr/titleAr.
 */
public record UpdateServiceCardRequest(
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
        Boolean visible) {}
