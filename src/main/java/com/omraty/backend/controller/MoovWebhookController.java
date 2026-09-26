package com.omraty.backend.controller;

import com.omraty.backend.dto.request.MoovWebhookPayload;
import com.omraty.backend.service.BookingPaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée de la confirmation de paiement envoyée par Moov. C'est la banque qui appelle, pas
 * l'app : la route est publique (permitAll sur /webhooks/**, voir SecurityConfig) car l'appel
 * n'arrive avec aucun JWT. Une vérification d'authenticité (secret partagé ou signature du corps)
 * viendra s'ajouter ici une fois la doc Moov reçue — en attendant, le seul garde-fou est que le
 * transactionId doit correspondre à un paiement réellement créé par le backend (voir
 * BookingPaymentService.confirmFromGateway).
 *
 * <p>Pas de préfixe /rooms ni /users : le webhook n'est pas une ressource métier de l'app.
 */
@RestController
@RequestMapping("/webhooks")
public class MoovWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MoovWebhookController.class);

    private final BookingPaymentService bookingPaymentService;

    public MoovWebhookController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    /**
     * Confirme (ou marque en échec) le paiement correspondant au transactionId reçu. Répond 200
     * sans corps : Moov n'attend qu'un accusé de réception. 404 si le transactionId est inconnu
     * (voir BookingPaymentException.PaymentNotFoundException) — rejouer le même webhook sur un
     * paiement déjà confirmé reste sans effet et répond 200 (idempotent).
     */
    @PostMapping("/moov")
    public ResponseEntity<Void> handleWebhook(@Valid @RequestBody MoovWebhookPayload payload) {
        log.info(
                "Webhook Moov reçu (transactionId={}, status={})",
                payload.transactionId(),
                payload.status());
        bookingPaymentService.confirmFromGateway(payload.transactionId(), payload.status());
        return ResponseEntity.ok().build();
    }
}
