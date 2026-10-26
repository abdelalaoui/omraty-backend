package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.CustomOfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proposition vue par son client (GET /users/me/custom-offers) : payment est la dernière tentative
 * de paiement (même forme que dans « مشترياتي »), null tant qu'aucune n'a été lancée.
 */
public record CustomOfferResponse(
        long id,
        String title,
        String description,
        BigDecimal amount,
        String packageLabel,
        LocalDateTime expiresAt,
        CustomOfferStatus status,
        LocalDateTime createdAt,
        PurchasePaymentResponse payment) {}
