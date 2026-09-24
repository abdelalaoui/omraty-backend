package com.omraty.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omraty.backend.dto.request.MoovWebhookPayload;
import com.omraty.backend.payment.WebhookVerifier;
import com.omraty.backend.service.BookingPaymentService;
import jakarta.validation.Validator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée de la confirmation de paiement envoyée par Moov. C'est la banque qui appelle, pas
 * l'app : la route est publique (permitAll sur /webhooks/**, voir SecurityConfig) car l'appel
 * n'arrive avec aucun JWT. {@link WebhookVerifier} authentifie l'appel (rejet en 401 si échec, voir
 * {@link #handleWebhook}) ; en plus de ça, le seul garde-fou est que le transactionId doit
 * correspondre à un paiement réellement créé par le backend (voir
 * BookingPaymentService.confirmFromGateway).
 *
 * <p>Pas de préfixe /rooms ni /users : le webhook n'est pas une ressource métier de l'app.
 */
@RestController
@RequestMapping("/webhooks")
public class MoovWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MoovWebhookController.class);

    private final BookingPaymentService bookingPaymentService;
    private final WebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MoovWebhookController(
            BookingPaymentService bookingPaymentService,
            WebhookVerifier webhookVerifier,
            ObjectMapper objectMapper,
            Validator validator) {
        this.bookingPaymentService = bookingPaymentService;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Confirme (ou marque en échec) le paiement correspondant au transactionId reçu. Le corps est
     * reçu brut (et non désérialisé directement par Spring) car {@link WebhookVerifier#verify} a
     * besoin des octets exacts envoyés par Moov pour une éventuelle vérification par signature.
     *
     * <p>401 si {@link WebhookVerifier#verify} échoue. 400 si le corps n'est pas un JSON valide de
     * {@link MoovWebhookPayload}. Répond 200 sans corps sinon : Moov n'attend qu'un accusé de
     * réception. 404 si le transactionId est inconnu (voir
     * BookingPaymentException.PaymentNotFoundException) — rejouer le même webhook sur un paiement
     * déjà confirmé reste sans effet et répond 200 (idempotent, voir tâche 9).
     */
    @PostMapping("/moov")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
        if (!webhookVerifier.verify(rawBody, headers)) {
            log.warn("Webhook Moov rejeté : échec de la vérification d'authenticité");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MoovWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, MoovWebhookPayload.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
        if (!validator.validate(payload).isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info(
                "Webhook Moov reçu (transactionId={}, status={})",
                payload.transactionId(),
                payload.status());
        bookingPaymentService.confirmFromGateway(payload.transactionId(), payload.status());
        return ResponseEntity.ok().build();
    }
}
