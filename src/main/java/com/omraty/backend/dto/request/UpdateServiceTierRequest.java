package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.ServiceTierType;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne basculer que closed
 * (fermeture temporaire) ou visible (masquage total) sans toucher au reste.
 */
public record UpdateServiceTierRequest(
        ServiceTierType type,
        Integer capacity,
        String label,
        Integer displayOrder,
        Boolean visible,
        Boolean closed) {}
