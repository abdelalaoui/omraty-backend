package com.omraty.backend.dto.request;

/**
 * Tous les champs sont optionnels : seuls ceux fournis (non null) sont modifiés (voir
 * PaymentMethodService.updatePaymentMethod). code n'est pas modifiable ; le logo se change via
 * PATCH /admin/payment-methods/{id}/logo.
 */
public record UpdatePaymentMethodRequest(
        String nameFr,
        String nameEn,
        String nameAr,
        String merchantNumber,
        String merchantName,
        String instructionsFr,
        String instructionsEn,
        String instructionsAr,
        Boolean enabled,
        Integer displayOrder) {}
