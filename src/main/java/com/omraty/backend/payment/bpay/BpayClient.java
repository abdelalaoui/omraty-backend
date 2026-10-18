package com.omraty.backend.payment.bpay;

/**
 * API B-PAY de Bankily (voir Spécification B-PAY v1.0) : le client génère un passcode dans son
 * application Bankily (B-PAY > code commerçant + montant + PIN), le saisit dans l'app avec son
 * numéro, et le backend appelle {@link #pay}. Authentification OAuth 2.0 (grant password, puis
 * refresh_token) gérée entièrement par l'implémentation — les appelants ne voient jamais le token.
 *
 * <p>{@link MockBpayClient} est actif par défaut (dev/test, voir payment.bpay.provider) ; {@link
 * HttpBpayClient} appelle la vraie API (test ou prod selon payment.bpay.base-url).
 */
public interface BpayClient {

    /**
     * POST /payment. Ne lève jamais d'exception pour un problème réseau : un délai dépassé ou une
     * réponse illisible donnent {@link BpayPaymentResult.Outcome#TIMEOUT}, puisque le paiement a pu
     * être exécuté côté Bankily — l'appelant doit alors interroger {@link #checkTransaction}.
     *
     * @param amount montant sans décimales superflues (ex. "120"), tel qu'attendu par Bankily.
     * @param language fr, en ou ar.
     */
    BpayPaymentResult pay(
            String clientPhone,
            String passcode,
            String amount,
            String language,
            String operationId);

    /** POST /checkTransaction, même garantie que {@link #pay} (TIMEOUT plutôt qu'une exception). */
    BpayCheckResult checkTransaction(String operationId);
}
