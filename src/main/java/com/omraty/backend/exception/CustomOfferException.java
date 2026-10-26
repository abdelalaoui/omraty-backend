package com.omraty.backend.exception;

public class CustomOfferException extends RuntimeException {

    CustomOfferException(String message) {
        super(message);
    }

    public static class CustomOfferNotFoundException extends CustomOfferException {
        public CustomOfferNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidCustomOfferRequestException extends CustomOfferException {
        public InvalidCustomOfferRequestException(String message) {
            super(message);
        }
    }

    /** La proposition n'est plus payable ou annulable (payée, expirée, paiement en cours...). */
    public static class CustomOfferStateException extends CustomOfferException {
        public CustomOfferStateException(String message) {
            super(message);
        }
    }
}
