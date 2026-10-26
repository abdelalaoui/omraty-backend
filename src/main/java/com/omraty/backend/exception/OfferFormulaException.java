package com.omraty.backend.exception;

public class OfferFormulaException extends RuntimeException {

    OfferFormulaException(String message) {
        super(message);
    }

    public static class InvalidOfferFormulaRequestException extends OfferFormulaException {
        public InvalidOfferFormulaRequestException(String message) {
            super(message);
        }
    }

    public static class OfferFormulaNotFoundException extends OfferFormulaException {
        public OfferFormulaNotFoundException(String message) {
            super(message);
        }
    }
}
