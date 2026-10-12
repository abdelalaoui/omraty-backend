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

    /** POST/PATCH /admin/payment-methods invalide (champ requis manquant, trop long...). */
    public static class InvalidPaymentMethodRequestException extends PaymentMethodException {
        public InvalidPaymentMethodRequestException(String message) {
            super(message);
        }
    }

    /** PATCH /admin/payment-methods/{id}[/logo] demandé pour un id inexistant. */
    public static class PaymentMethodNotFoundException extends PaymentMethodException {
        public PaymentMethodNotFoundException(String message) {
            super(message);
        }
    }

    /** POST /admin/payment-methods : code déjà utilisé par un autre moyen de paiement. */
    public static class DuplicatePaymentMethodCodeException extends PaymentMethodException {
        public DuplicatePaymentMethodCodeException(String message) {
            super(message);
        }
    }
}
