package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

/**
 * GET/POST/PATCH /admin/payment-methods : les 3 variantes de chaque champ traduit (contrairement à
 * PaymentMethodResponse, résolu dans une seule langue), pour permettre l'édition complète depuis le
 * panel admin.
 */
public record AdminPaymentMethodResponse(
        long id,
        String code,
        String nameFr,
        String nameEn,
        String nameAr,
        String merchantNumber,
        String merchantName,
        String instructionsFr,
        String instructionsEn,
        String instructionsAr,
        String logoUrl,
        boolean enabled,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
