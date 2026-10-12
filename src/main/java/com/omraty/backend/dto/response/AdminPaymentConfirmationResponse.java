package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Renvoyé par POST /admin/payments/{id}/confirm et /reject : confirmedByAdminId/confirmedAt ne sont
 * renseignés qu'après une confirmation ; rejectionReason après un rejet (null sinon).
 *
 * <p>hasInvoice/invoiceNumber (voir carte "Facture panel admin") : la confirmation appelle
 * InvoiceService.generateIfFullyPaid, ces champs permettent au panel d'afficher le lien vers la
 * facture immédiatement si l'achat vient de passer à intégralement payé — sans appel séparé.
 */
public record AdminPaymentConfirmationResponse(
        long id,
        PaymentStatus status,
        UUID confirmedByAdminId,
        LocalDateTime confirmedAt,
        String rejectionReason,
        boolean hasInvoice,
        String invoiceNumber) {}
