package com.omraty.backend.payment.bpay;

/**
 * Résultat de POST /checkTransaction (voir BpayClient.checkTransaction). status est le champ brut
 * de Bankily (TS = succès, TF = échouée, TA = en suspens), traduit dans {@link #outcome}.
 */
public record BpayCheckResult(
        Outcome outcome, String errorCode, String errorMessage, String transactionId) {

    public enum Outcome {
        /** Statut TS. */
        SUCCESS,
        /** Statut TF. */
        FAILED,
        /** Statut TA : transaction en suspens chez Bankily, à revérifier plus tard. */
        AMBIGUOUS,
        /**
         * errorCode ≠ 0 : Bankily n'a pas pu répondre sur cette opération (le plus souvent,
         * operationId inconnu parce que l'appel /payment ne lui est jamais parvenu).
         */
        ERROR,
        /** Délai dépassé ou réponse illisible. */
        TIMEOUT
    }

    public static BpayCheckResult of(Outcome outcome, String transactionId) {
        return new BpayCheckResult(outcome, "0", null, transactionId);
    }

    public static BpayCheckResult error(String errorCode, String errorMessage) {
        return new BpayCheckResult(Outcome.ERROR, errorCode, errorMessage, null);
    }

    public static BpayCheckResult timeout() {
        return new BpayCheckResult(Outcome.TIMEOUT, null, null, null);
    }
}
