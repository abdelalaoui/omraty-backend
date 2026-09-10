package com.omraty.backend.dto.request;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne basculer que comingSoon
 * (mode désactivé/popup) ou visible (masquer la carte) sans toucher au reste du contenu.
 */
public record UpdateServiceCardRequest(
        String type,
        String title,
        String description,
        String buttonText,
        String icon,
        Boolean comingSoon,
        Boolean visible) {}
