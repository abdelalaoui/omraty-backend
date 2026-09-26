package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan de paiement d'un achat de chambre entière (roomId) ou d'une réservation de lit (bedId) —
 * exactement l'un des deux renseigné, jamais les deux (voir migration V30, CHECK
 * chk_booking_payment_exactly_one_target). plan = FULL : payé intégralement à la création, aucune
 * ligne BookingInstallment ; plan = INSTALLMENTS : 3 tranches, voir BookingInstallment. status :
 * voir PaymentStatus (migration V32) — démarre PENDING jusqu'à confirmation par la passerelle de
 * paiement. moovPaymentCode/moovTransactionId/payerPhone/expiresAt : référence du paiement côté
 * Moov (migration V33), renseignés une fois le paiement créé côté passerelle — nullables tant que
 * ce n'est pas le cas. moovTransactionId est unique en base, c'est la clé d'idempotence du webhook.
 */
public record BookingPayment(
        long id,
        Long roomId,
        Long bedId,
        PaymentPlan plan,
        PaymentStatus status,
        BigDecimal totalAmount,
        String moovPaymentCode,
        String moovTransactionId,
        String payerPhone,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {}
