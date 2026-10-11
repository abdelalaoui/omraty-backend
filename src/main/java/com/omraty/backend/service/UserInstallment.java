package com.omraty.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une tranche d'un plan de paiement INSTALLMENTS, pour GET /users/me/purchases.
 * paymentMethod/paymentReference/declaredAt (voir migration V51) : renseignés uniquement pour les
 * tranches 2/3 d'un achat payé via un moyen de paiement manuel (la 1ère est représentée par
 * booking_payment, voir BookingPaymentService.createManualInstallments) — null sinon.
 * merchantNumber est celui du moyen de paiement choisi pour l'achat entier (voir
 * BookingPaymentService.toPurchasePayment).
 */
public record UserInstallment(
        long id,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String paymentMethod,
        String paymentReference,
        String merchantNumber,
        LocalDateTime declaredAt) {}
