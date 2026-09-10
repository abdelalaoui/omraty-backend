package com.omraty.backend.exception;

public class PackageException extends RuntimeException {

    PackageException(String message) {
        super(message);
    }

    public static class InvalidPackageRequestException extends PackageException {
        public InvalidPackageRequestException(String message) {
            super(message);
        }
    }

    public static class PackageNotFoundException extends PackageException {
        public PackageNotFoundException(String message) {
            super(message);
        }
    }
}
