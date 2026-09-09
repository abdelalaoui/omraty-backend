package com.omraty.backend.exception;

public class BenefitException extends RuntimeException {

    BenefitException(String message) {
        super(message);
    }

    public static class InvalidBenefitRequestException extends BenefitException {
        public InvalidBenefitRequestException(String message) {
            super(message);
        }
    }

    public static class BenefitNotFoundException extends BenefitException {
        public BenefitNotFoundException(String message) {
            super(message);
        }
    }
}
