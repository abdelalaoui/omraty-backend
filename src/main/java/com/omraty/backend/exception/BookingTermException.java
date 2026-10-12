package com.omraty.backend.exception;

public class BookingTermException extends RuntimeException {

    BookingTermException(String message) {
        super(message);
    }

    /** POST/PATCH/PUT /admin/booking-terms invalide (champ requis manquant, doublon...). */
    public static class InvalidBookingTermRequestException extends BookingTermException {
        public InvalidBookingTermRequestException(String message) {
            super(message);
        }
    }

    public static class BookingTermNotFoundException extends BookingTermException {
        public BookingTermNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * acceptedTermsVersion envoyé à la création d'un paiement (voir BookingPaymentService) ne
     * correspond plus à la version actuelle des conditions (modifiées entre l'affichage et la
     * réservation) — 409 : l'app doit recharger GET /booking-terms et refaire accepter le client.
     */
    public static class TermsChangedException extends BookingTermException {
        public TermsChangedException(String message) {
            super(message);
        }
    }
}
