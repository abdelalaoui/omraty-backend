package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * displayOrder/visible sont optionnels : par défaut l'avantage est ajouté visible, à la fin de la
 * liste (ordre = max existant + 1).
 */
public record CreateBenefitRequest(
        @NotBlank(message = "L'icône est requise") String icon,
        @NotBlank(message = "Le libellé est requis") String label,
        Integer displayOrder,
        Boolean visible) {}
