package com.omraty.backend.dto.request;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne changer que l'ordre, ou
 * de masquer une bannière via visible = false sans toucher au reste. L'image se change via un
 * endpoint dédié (multipart), pas ici.
 */
public record UpdateBannerRequest(
        String title, String description, Integer displayOrder, Boolean visible) {}
