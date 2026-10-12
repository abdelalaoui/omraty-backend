package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Renvoyé par POST /admin/payments/{id}/confirm et /reject : confirmedByAdminId/confirmedAt ne sont
 * renseignés qu'après une confirmation ; rejectionReason après un rejet (null sinon).
 */
public record AdminPaymentConfirmationResponse(
        long id,
        PaymentStatus status,
        UUID confirmedByAdminId,
        LocalDateTime confirmedAt,
        String rejectionReason) {}
