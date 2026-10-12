package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une tranche (1/2/3) d'un plan de paiement INSTALLMENTS. paidAt null = pas encore payée.
 * paymentMethod/paymentReference/declaredAt : renseignés uniquement pour les tranches 2/3 d'un
 * achat payé via un moyen de paiement manuel (null sinon, voir
 * BookingPaymentService.createManualInstallments). merchantNumber est celui du moyen de paiement
 * choisi pour l'achat entier.
 */
public record InstallmentResponse(
        long id,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String paymentMethod,
        String paymentReference,
        String merchantNumber,
        LocalDateTime declaredAt) {}
