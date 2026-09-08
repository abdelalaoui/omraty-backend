package com.omraty.backend.exception;

public class BannerException extends RuntimeException {

    BannerException(String message) {
        super(message);
    }

    public static class InvalidBannerRequestException extends BannerException {
        public InvalidBannerRequestException(String message) {
            super(message);
        }
    }

    public static class BannerNotFoundException extends BannerException {
        public BannerNotFoundException(String message) {
            super(message);
        }
    }
}
