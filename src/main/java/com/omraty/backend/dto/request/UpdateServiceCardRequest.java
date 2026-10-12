package com.omraty.backend.dto.request;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne basculer que comingSoon
 * (mode désactivé/popup) ou visible (masquer la carte) sans toucher au reste du contenu, ou de ne
 * traduire que titleEn sans toucher à titleFr/titleAr.
 *
 * <p>titleEn/titleAr, descriptionEn/descriptionAr et buttonTextEn/buttonTextAr retombent sur leur
 * variante _fr tant qu'ils ne sont pas traduits (voir ServiceCardMapper) : null signifiant déjà «
 * inchangé », il n'existe pas d'autre valeur permettant de revenir à cet état « non traduit ». Les
 * flags clearTitleEn/clearTitleAr/clearDescriptionEn/clearDescriptionAr/clearButtonTextEn/
 * clearButtonTextAr couvrent ce cas : à true, le champ correspondant est remis à null (incompatible
 * avec la fourniture d'une valeur pour ce même champ dans la même requête).
 */
public record UpdateServiceCardRequest(
        String type,
        String titleFr,
        String titleEn,
        Boolean clearTitleEn,
        String titleAr,
        Boolean clearTitleAr,
        String descriptionFr,
        String descriptionEn,
        Boolean clearDescriptionEn,
        String descriptionAr,
        Boolean clearDescriptionAr,
        String buttonTextFr,
        String buttonTextEn,
        Boolean clearButtonTextEn,
        String buttonTextAr,
        Boolean clearButtonTextAr,
        String icon,
        String imageUrl,
        Boolean comingSoon,
        Boolean visible) {}
