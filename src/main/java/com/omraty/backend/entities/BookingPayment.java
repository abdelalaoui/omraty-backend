package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.PaymentPlan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan de paiement d'un achat de chambre entière (roomId) ou d'une réservation de lit (bedId) —
 * exactement l'un des deux renseigné, jamais les deux (voir migration V25, CHECK
 * chk_booking_payment_exactly_one_target). plan = FULL : payé intégralement à la création, aucune
 * ligne BookingInstallment ; plan = INSTALLMENTS : 3 tranches, voir BookingInstallment.
 */
public record BookingPayment(
        long id,
        Long roomId,
        Long bedId,
        PaymentPlan plan,
        BigDecimal totalAmount,
        LocalDateTime createdAt) {}
