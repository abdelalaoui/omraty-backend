package com.omraty.backend.exception;

public class AgencyCodeException extends RuntimeException {

    AgencyCodeException(String message) {
        super(message);
    }

    public static class InvalidAgencyCodeRequestException extends AgencyCodeException {
        public InvalidAgencyCodeRequestException(String message) {
            super(message);
        }
    }

    public static class AgencyCodeNotFoundException extends AgencyCodeException {
        public AgencyCodeNotFoundException(String message) {
            super(message);
        }
    }

    /** Le code a déjà été validé par un compte (usage unique). */
    public static class AgencyCodeAlreadyUsedException extends AgencyCodeException {
        public AgencyCodeAlreadyUsedException(String message) {
            super(message);
        }
    }
}
