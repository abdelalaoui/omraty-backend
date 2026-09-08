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
}
