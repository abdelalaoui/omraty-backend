package com.omraty.backend.payment;

import java.util.Map;

/**
 * Vérifie l'authenticité d'un appel entrant au webhook de confirmation de paiement (voir
 * MoovWebhookController) : /webhooks/moov est public (permitAll, voir SecurityConfig), donc sans ce
 * garde-fou n'importe qui pourrait prétendre qu'un paiement est confirmé sans avoir payé. Le
 * mécanisme réel (signature HMAC, secret partagé en en-tête, IP fixe...) dépend de ce que fournit
 * Moov et n'est pas encore connu.
 *
 * <p>{@link NoOpWebhookVerifier} est l'implémentation active par défaut (dev/test, voir
 * payment.gateway.provider) ; une implémentation MoovWebhookVerifier (annotée
 * {@code @ConditionalOnProperty(prefix = "payment.gateway", name = "provider", havingValue =
 * "moov")}) viendra s'ajouter ici une fois la doc Moov reçue, sans changer l'appelant.
 */
public interface WebhookVerifier {

    /**
     * @param rawBody corps brut de la requête, tel que reçu (nécessaire pour une vérification par
     *     signature, qui porte sur les octets exacts envoyés).
     * @param headers en-têtes de la requête (potentielle signature/secret transmis par Moov).
     * @return true si l'appel est authentifié comme venant de Moov.
     */
    boolean verify(String rawBody, Map<String, String> headers);
}
