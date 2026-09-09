package com.omraty.backend.dto.request;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne changer que le libellé,
 * ou de masquer un avantage via visible = false sans toucher au reste.
 */
public record UpdateBenefitRequest(
        String icon, String label, Integer displayOrder, Boolean visible) {}
