package com.omraty.backend.service;

import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail du plan de paiement d'une réservation (GET /users/me/purchases). id est celui de
 * booking_payment, nécessaire à l'app pour appeler un futur endpoint de téléchargement de facture.
 * installments est vide pour plan = FULL (payé intégralement à la confirmation, aucune tranche).
 * nextDueDate = échéance de la 1ère tranche non payée, null si tout est payé (voir
 * BookingPaymentService.toPurchasePayment).
 *
 * <p>paymentMethod/paymentReference/merchantNumber/declaredAt (voir migrations V50/V51) :
 * renseignés uniquement pour un achat payé via un moyen de paiement manuel (Moov, Bankily... voir
 * PaymentMethod), null pour un achat via la passerelle Moov — permettent au client de revenir
 * copier son code. status reflète celui de booking_payment (voir PaymentStatus), notamment
 * PENDING_VERIFICATION une fois le paiement déclaré.
 */
public record UserPurchasePayment(
        long id,
        PaymentPlan plan,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate nextDueDate,
        List<UserInstallment> installments,
        String paymentMethod,
        String paymentReference,
        String merchantNumber,
        PaymentStatus status,
        LocalDateTime declaredAt) {}
