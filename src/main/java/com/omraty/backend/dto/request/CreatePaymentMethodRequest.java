package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * code, nameFr et merchantNumber sont requis ; nameEn/nameAr et instructions* sont optionnels
 * (repli sur la variante fr tant qu'ils ne sont pas traduits, voir PaymentMethodMapper).
 * displayOrder/enabled non fournis = valeurs par défaut (0, désactivé — voir
 * PaymentMethodService.createPaymentMethod).
 */
public record CreatePaymentMethodRequest(
        @NotBlank(message = "Le code est requis") String code,
        @NotBlank(message = "Le nom en français est requis") String nameFr,
        String nameEn,
        String nameAr,
        @NotBlank(message = "Le numéro marchand est requis") String merchantNumber,
        String merchantName,
        String instructionsFr,
        String instructionsEn,
        String instructionsAr,
        Integer displayOrder,
        Boolean enabled) {}
