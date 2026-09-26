package com.omraty.backend.payment;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation factice de {@link WebhookVerifier}, active par défaut (dev/test, voir
 * payment.gateway.provider) : accepte tout appel sans aucune vérification, pour développer et
 * tester le flux de webhook tant que le mécanisme réel (signature, secret partagé...) n'est pas
 * connu côté Moov. Définir payment.gateway.provider=moov pour basculer vers la vraie implémentation
 * une fois la doc Moov reçue — <strong>ne jamais utiliser cette implémentation en
 * production</strong>, d'où le warning à chaque appel.
 */
@Service
@ConditionalOnProperty(
        prefix = "payment.gateway",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true)
public class NoOpWebhookVerifier implements WebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(NoOpWebhookVerifier.class);

    @Override
    public boolean verify(String rawBody, Map<String, String> headers) {
        log.warn(
                "WebhookVerifier factice actif : AUCUNE vérification de signature sur le webhook"
                        + " Moov. Ne jamais utiliser en production.");
        return true;
    }
}
