package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.PaymentStatus;
import java.time.LocalDateTime;

/**
 * Renvoyé par GET /payments/{id} : l'app interroge cet endpoint en arrière-plan pendant l'attente
 * de la confirmation asynchrone du paiement (webhook Moov, voir MoovWebhookController) jusqu'à ce
 * que status passe à CONFIRMED ou FAILED (voir BookingPaymentService.getStatusForUser).
 */
public record PaymentStatusResponse(
        long id, PaymentStatus status, String paymentCode, LocalDateTime expiresAt) {}
