package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.BpayTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une tentative de paiement B-PAY (voir migration V61, BpayPaymentService). installmentId renseigné
 * pour une tranche 2/3, null pour le paiement dû à la création. errorCode/errorMessage/
 * transactionId sont ceux renvoyés par Bankily.
 */
public record BpayTransaction(
        long id,
        long bookingPaymentId,
        Long installmentId,
        String operationId,
        String clientPhone,
        BigDecimal amount,
        BpayTransactionStatus status,
        String errorCode,
        String errorMessage,
        String transactionId,
        int checkCount,
        LocalDateTime lastCheckedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
