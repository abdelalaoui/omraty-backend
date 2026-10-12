package com.omraty.backend.service;

import com.omraty.backend.dto.response.BookingTermItemResponse;
import com.omraty.backend.entities.enums.PaymentPlan;
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
 * <p>acceptedTermsVersion/acceptedTermsAt/acceptedTerms (migration V55, carte "Conditions de
 * réservation gérées depuis le serveur") : trace de l'acceptation des conditions au moment de la
 * réservation — null/vide pour un achat antérieur à cette carte ou une offre VIP.
 */
public record UserPurchasePayment(
        long id,
        PaymentPlan plan,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate nextDueDate,
        List<UserInstallment> installments,
        String acceptedTermsVersion,
        LocalDateTime acceptedTermsAt,
        List<BookingTermItemResponse> acceptedTerms) {}
