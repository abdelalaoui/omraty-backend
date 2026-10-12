package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail du plan de paiement d'une réservation (GET /users/me/purchases). id est celui de
 * booking_payment, nécessaire à l'app pour appeler un futur endpoint de téléchargement de facture.
 * installments est vide pour plan = FULL (payé intégralement à la confirmation). nextDueDate =
 * échéance de la 1ère tranche non payée, null si tout est payé.
 *
 * <p>paymentMethod/paymentReference/merchantNumber/declaredAt : renseignés uniquement pour un achat
 * payé via un moyen de paiement manuel (Moov, Bankily...), null pour un achat via la passerelle
 * Moov — permettent au client de revenir copier son code.
 *
 * <p>hasInvoice/invoiceNumber/invoiceGeneratedAt : renseignés une fois l'achat intégralement payé
 * (voir InvoiceService) — permettent d'afficher le bouton « Facture » sans appel séparé.
 */
public record PurchasePaymentResponse(
        long id,
        PaymentPlan plan,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate nextDueDate,
        List<InstallmentResponse> installments,
        String paymentMethod,
        String paymentReference,
        String merchantNumber,
        PaymentStatus status,
        LocalDateTime declaredAt,
        boolean hasInvoice,
        String invoiceNumber,
        LocalDateTime invoiceGeneratedAt) {}
