package com.omraty.backend.exception;

public class ServiceTierException extends RuntimeException {

    ServiceTierException(String message) {
        super(message);
    }

    public static class InvalidServiceTierRequestException extends ServiceTierException {
        public InvalidServiceTierRequestException(String message) {
            super(message);
        }
    }

    public static class ServiceTierNotFoundException extends ServiceTierException {
        public ServiceTierNotFoundException(String message) {
            super(message);
        }
    }
}
