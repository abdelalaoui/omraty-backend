package com.omraty.backend.payment.bpay;

/**
 * Résultat de POST /payment (voir BpayClient.pay). errorCode/errorMessage/transactionId sont les
 * champs bruts de Bankily (errorCode 0 = succès, 1 = autre erreur, 2 = token invalide, 4 =
 * operationId requis) ; errorMessage est déjà dans la langue envoyée à l'appel.
 */
public record BpayPaymentResult(
        Outcome outcome, String errorCode, String errorMessage, String transactionId) {

    /**
     * errorCode propre à l'app (jamais renvoyé par Bankily) : authentification B-PAY impossible,
     * /payment n'a pas été appelé.
     */
    public static final String AUTH_ERROR_CODE = "AUTH";

    public enum Outcome {
        /** errorCode = 0 : paiement exécuté. */
        SUCCESS,
        /** errorCode ≠ 0 : refusé par Bankily (passcode, téléphone, montant invalide...). */
        ERROR,
        /** Délai dépassé ou réponse illisible : résultat inconnu, à vérifier. */
        TIMEOUT
    }

    public static BpayPaymentResult success(String transactionId) {
        return new BpayPaymentResult(Outcome.SUCCESS, "0", null, transactionId);
    }

    public static BpayPaymentResult error(String errorCode, String errorMessage) {
        return new BpayPaymentResult(Outcome.ERROR, errorCode, errorMessage, null);
    }

    public static BpayPaymentResult timeout() {
        return new BpayPaymentResult(Outcome.TIMEOUT, null, null, null);
    }
}
