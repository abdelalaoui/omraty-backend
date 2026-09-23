package com.omraty.backend.controller;

import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.service.BookingPaymentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statut d'un paiement, pour que l'app sache quand afficher « Paiement réussi ». La confirmation
 * arrive de façon asynchrone (webhook Moov, voir MoovWebhookController) : c'est ici que l'app
 * interroge en arrière-plan (polling) jusqu'à ce que le statut passe à CONFIRMED/FAILED. Ne dépend
 * pas de l'API Moov : ne lit que booking_payment (voir BookingPaymentService.getStatusForUser).
 * Accessible à tout utilisateur authentifié, mais uniquement pour ses propres paiements.
 */
@RestController
public class PaymentController {

    private final BookingPaymentService bookingPaymentService;

    public PaymentController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentStatusResponse> getStatus(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        return ResponseEntity.ok(bookingPaymentService.getStatusForUser(userId, id));
    }
}
