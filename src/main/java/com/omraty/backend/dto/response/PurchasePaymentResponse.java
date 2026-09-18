package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentPlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Détail du plan de paiement d'une réservation (GET /users/me/purchases). installments est vide
 * pour plan = FULL (payé intégralement à la confirmation). nextDueDate = échéance de la 1ère
 * tranche non payée, null si tout est payé.
 */
public record PurchasePaymentResponse(
        PaymentPlan plan,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate nextDueDate,
        List<InstallmentResponse> installments) {}
