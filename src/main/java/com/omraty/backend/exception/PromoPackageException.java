package com.omraty.backend.exception;

public class PromoPackageException extends RuntimeException {

    PromoPackageException(String message) {
        super(message);
    }

    public static class InvalidPromoPackageRequestException extends PromoPackageException {
        public InvalidPromoPackageRequestException(String message) {
            super(message);
        }
    }

    public static class PromoPackageNotFoundException extends PromoPackageException {
        public PromoPackageNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidPromoPackageTierRequestException extends PromoPackageException {
        public InvalidPromoPackageTierRequestException(String message) {
            super(message);
        }
    }

    public static class PromoPackageTierNotFoundException extends PromoPackageException {
        public PromoPackageTierNotFoundException(String message) {
            super(message);
        }
    }
}
