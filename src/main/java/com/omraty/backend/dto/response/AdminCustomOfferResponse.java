package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.CustomOfferStatus;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Proposition vue par l'admin (GET /admin/custom-offers). */
public record AdminCustomOfferResponse(
        long id,
        UUID userId,
        String userPhone,
        String title,
        String description,
        BigDecimal amount,
        Long packageId,
        String packageLabel,
        LocalDateTime expiresAt,
        CustomOfferStatus status,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt) {}
