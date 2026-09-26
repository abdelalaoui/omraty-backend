package com.omraty.backend.exception;

public class BookingPaymentException extends RuntimeException {

    BookingPaymentException(String message) {
        super(message);
    }

    /** Le prix de la formule ROOM correspondante n'a pas encore été saisi par l'admin. */
    public static class PriceNotConfiguredException extends BookingPaymentException {
        public PriceNotConfiguredException(String message) {
            super(message);
        }
    }

    /**
     * Plan INSTALLMENTS demandé mais le package n'a pas de endDate : les échéances des tranches 2/3
     * en dépendent (voir BookingPaymentService).
     */
    public static class PackageDatesMissingException extends BookingPaymentException {
        public PackageDatesMissingException(String message) {
            super(message);
        }
    }

    /**
     * Paiement introuvable, dans deux cas : aucun booking_payment ne porte le transactionId reçu
     * dans le webhook Moov (voir BookingPaymentService.confirmFromGateway) ; ou GET /payments/{id}
     * demandé pour un id inexistant ou qui n'appartient pas à l'utilisateur authentifié (voir
     * BookingPaymentService.getStatusForUser) — même id renvoyé pour les deux cas, pour ne pas
     * révéler l'existence du paiement d'un autre utilisateur (comme
     * NotificationService.markAsRead).
     */
    public static class PaymentNotFoundException extends BookingPaymentException {
        public PaymentNotFoundException(String message) {
            super(message);
        }
    }

    public static class InstallmentNotFoundException extends BookingPaymentException {
        public InstallmentNotFoundException(String message) {
            super(message);
        }
    }

    public static class InstallmentAlreadyPaidException extends BookingPaymentException {
        public InstallmentAlreadyPaidException(String message) {
            super(message);
        }
    }
}
