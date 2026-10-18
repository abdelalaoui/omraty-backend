package com.omraty.backend.dto.response;

/**
 * Renvoyé par GET /payment-methods (public) : name/instructions dans la langue du header
 * Accept-Language de la requête (voir PaymentMethodMapper). merchantNumber est le numéro à
 * afficher/copier dans l'app pour que le client y transfère son paiement.
 */
public record PaymentMethodResponse(
        String code,
        String name,
        String logoUrl,
        String merchantNumber,
        String merchantName,
        String instructions,
        String integration) {

    /** Transfert manuel + « J'ai payé », vérifié par un admin. */
    public static final String INTEGRATION_MANUAL = "MANUAL";

    /** Paiement B-PAY (Bankily) : numéro + passcode saisis dans l'app, confirmé automatiquement. */
    public static final String INTEGRATION_BPAY = "BPAY";
}
