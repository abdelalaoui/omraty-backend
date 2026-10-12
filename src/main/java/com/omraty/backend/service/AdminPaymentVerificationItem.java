package com.omraty.backend.service;

import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un paiement ou une tranche manuel(le) à vérifier par l'admin (GET /admin/payments, voir
 * BookingPaymentService.listPaymentsForAdmin) : la file mélange les deux granularités, puisque
 * tranche 1 (représentée par booking_payment) et tranches 2/3 (leur propre ligne
 * booking_installment) se confirment/rejettent via des routes différentes (voir
 * AdminPaymentController, AdminInstallmentController).
 *
 * <p>id = "payment-10" ou "installment-23" (préfixe distinguant la route à appeler, même convention
 * que RoomService.OwnedPurchase). installmentId/sequence restent null pour un paiement en plan FULL
 * (ou la 1ère tranche d'un plan INSTALLMENTS). status est celui de booking_payment pour un item
 * "payment", synthétisé (PENDING_VERIFICATION si declaredAt renseigné, sinon PENDING) pour un item
 * "installment" — une tranche n'a pas de colonne status propre.
 */
public record AdminPaymentVerificationItem(
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
        LocalDateTime queuedAt,
        String purchaseDescription) {}
