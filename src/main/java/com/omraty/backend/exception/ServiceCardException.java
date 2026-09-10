package com.omraty.backend.exception;

public class ServiceCardException extends RuntimeException {

    ServiceCardException(String message) {
        super(message);
    }

    public static class InvalidServiceCardRequestException extends ServiceCardException {
        public InvalidServiceCardRequestException(String message) {
            super(message);
        }
    }

    public static class ServiceCardNotFoundException extends ServiceCardException {
        public ServiceCardNotFoundException(String message) {
            super(message);
        }
    }
}
