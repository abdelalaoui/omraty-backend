package com.omraty.backend.exception;

public class TripPackageException extends RuntimeException {

    TripPackageException(String message) {
        super(message);
    }

    public static class InvalidTripPackageRequestException extends TripPackageException {
        public InvalidTripPackageRequestException(String message) {
            super(message);
        }
    }

    public static class TripPackageNotFoundException extends TripPackageException {
        public TripPackageNotFoundException(String message) {
            super(message);
        }
    }
}
