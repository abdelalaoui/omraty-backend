package com.omraty.backend.exception;

public class PaymentMethodException extends RuntimeException {

    PaymentMethodException(String message) {
        super(message);
    }

    /**
     * paymentMethod inconnu ou désactivé, demandé à la création d'un paiement (voir
     * BookingPaymentService) — 400, jamais 404 : contrairement à un paiement, un moyen de paiement
     * n'appartient à personne, il n'y a pas de fuite d'existence à éviter.
     */
    public static class UnknownOrDisabledPaymentMethodException extends PaymentMethodException {
        public UnknownOrDisabledPaymentMethodException(String message) {
            super(message);
        }
    }
}
