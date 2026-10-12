package com.omraty.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook Meta WhatsApp (voir WhatsApp Manager > Configuration > Webhooks) : requis par Meta pour
 * finaliser la configuration production d'un numéro, même si l'app n'exploite aucun événement
 * entrant — elle n'envoie que des codes OTP sortants (voir MetaWhatsAppOtpSender). Route publique
 * (permitAll sur /webhooks/**, voir SecurityConfig), comme MoovWebhookController : c'est Meta qui
 * appelle, sans JWT.
 */
@RestController
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final String verifyToken;

    public WhatsAppWebhookController(
            @Value("${whatsapp.webhook.verify-token:}") String verifyToken) {
        this.verifyToken = verifyToken;
    }

    /**
     * Handshake de vérification exigé par Meta à la configuration du webhook : renvoie
     * hub.challenge tel quel si hub.verify_token correspond à whatsapp.webhook.verify-token, sinon
     * 403 (y compris si ce réglage est vide — pas de vérification désactivée par défaut).
     */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if (verifyToken.isBlank() || !"subscribe".equals(mode) || !verifyToken.equals(token)) {
            log.warn("Webhook WhatsApp : échec de la vérification (mode={})", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(challenge);
    }

    /**
     * Événements WhatsApp (statuts de message, messages entrants). Aucun n'est exploité pour
     * l'instant — juste un accusé de réception pour éviter que Meta ne renvoie indéfiniment le même
     * événement.
     */
    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String rawBody) {
        log.info("Webhook WhatsApp reçu (ignoré, voir Javadoc de la classe) : {}", rawBody);
        return ResponseEntity.ok().build();
    }
}
