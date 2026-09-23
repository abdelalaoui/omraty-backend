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
     * Aucun booking_payment ne porte le transactionId reçu dans le webhook Moov (voir
     * BookingPaymentService.confirmFromGateway) : transactionId erroné, paiement jamais créé côté
     * backend, ou webhook adressé au mauvais environnement.
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
