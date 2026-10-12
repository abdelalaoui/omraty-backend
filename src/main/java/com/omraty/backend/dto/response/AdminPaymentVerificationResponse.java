package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un paiement ou une tranche manuel(le) à vérifier (GET /admin/payments, voir
 * BookingPaymentService.listPaymentsForAdmin). id = "payment-10" ou "installment-23" : préfixe
 * indiquant la route à appeler pour confirmer/rejeter (POST /admin/payments/{bookingPaymentId}
 * /confirm|reject pour "payment-", PATCH /admin/installments/{installmentId}/mark-paid pour
 * "installment-", voir installmentId).
 */
public record AdminPaymentVerificationResponse(
        String id,
        long bookingPaymentId,
        Long installmentId,
        Integer sequence,
        UUID userId,
        String userPhone,
        String paymentMethod,
        String paymentReference,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime declaredAt,
        String purchaseDescription) {}
