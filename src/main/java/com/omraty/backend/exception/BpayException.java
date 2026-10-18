package com.omraty.backend.exception;

public class BpayException extends RuntimeException {

    BpayException(String message) {
        super(message);
    }

    /** Le paiement (ou la tranche) n'a pas été créé avec le moyen de paiement B-PAY. */
    public static class NotBpayPaymentException extends BpayException {
        public NotBpayPaymentException(String message) {
            super(message);
        }
    }

    /**
     * Rien à payer : déjà payé, expiré, ou une tentative B-PAY est déjà en cours de traitement
     * (voir BookingPaymentService#lockForBpay).
     */
    public static class PaymentNotPayableException extends BpayException {
        public PaymentNotPayableException(String message) {
            super(message);
        }
    }
}
