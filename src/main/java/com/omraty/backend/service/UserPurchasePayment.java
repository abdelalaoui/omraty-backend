package com.omraty.backend.service;

import com.omraty.backend.entities.enums.PaymentPlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Détail du plan de paiement d'une réservation (GET /users/me/purchases). id est celui de
 * booking_payment, nécessaire à l'app pour appeler un futur endpoint de téléchargement de facture.
 * installments est vide pour plan = FULL (payé intégralement à la confirmation, aucune tranche).
 * nextDueDate = échéance de la 1ère tranche non payée, null si tout est payé (voir
 * BookingPaymentService.toPurchasePayment).
 */
public record UserPurchasePayment(
        long id,
        PaymentPlan plan,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate nextDueDate,
        List<UserInstallment> installments) {}
