package com.omraty.backend.exception;

public class UserException extends RuntimeException {

    UserException(String message) {
        super(message);
    }

    UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class InvalidIdentityRequestException extends UserException {
        public InvalidIdentityRequestException(String message) {
            super(message);
        }
    }

    public static class PhotoStorageException extends UserException {
        public PhotoStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UserNotFoundException extends UserException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class IdentityNotPendingException extends UserException {
        public IdentityNotPendingException(String message) {
            super(message);
        }
    }

    /**
     * NNI déjà enregistré pour une autre personne (compte ou membre de famille) : une personne ne
     * participe qu'une fois à la tombola (voir vue draw_participant, migration V65).
     */
    public static class NniAlreadyUsedException extends UserException {
        public NniAlreadyUsedException(String message) {
            super(message);
        }
    }
}
