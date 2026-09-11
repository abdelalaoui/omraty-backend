package com.omraty.backend.exception;

public class VipRequestException extends RuntimeException {

    VipRequestException(String message) {
        super(message);
    }

    public static class InvalidVipRequestException extends VipRequestException {
        public InvalidVipRequestException(String message) {
            super(message);
        }
    }

    public static class VipRequestNotFoundException extends VipRequestException {
        public VipRequestNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * La demande n'est pas dans l'état requis pour cette action (déjà traitée, offre expirée...).
     */
    public static class VipRequestStateException extends VipRequestException {
        public VipRequestStateException(String message) {
            super(message);
        }
    }
}
