package com.omraty.backend.payment;

import java.math.BigDecimal;

/**
 * Abstraction de la passerelle de paiement mobile money (Moov) : isole les appels externes pour ne
 * pas bloquer le reste du flux de paiement tant que la doc de l'API réelle n'est pas reçue côté
 * banque. {@link MockPaymentGatewayClient} est l'implémentation active par défaut (dev/test, voir
 * payment.gateway.provider) ; une implémentation MoovPaymentGatewayClient (annotée
 * {@code @ConditionalOnProperty(prefix = "payment.gateway", name = "provider", havingValue =
 * "moov")}) viendra s'ajouter ici une fois la doc reçue, sans changer les appelants.
 */
public interface PaymentGatewayClient {

    /**
     * Crée un paiement côté passerelle. internalReference identifie l'achat côté booking_payment.
     */
    PaymentGatewayResult createPayment(String phone, BigDecimal amount, String internalReference);

    /** Statut courant d'une transaction précédemment créée par {@link #createPayment}. */
    PaymentGatewayStatus checkStatus(String transactionId);
}
