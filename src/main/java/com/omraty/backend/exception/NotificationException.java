package com.omraty.backend.exception;

public class NotificationException extends RuntimeException {

    NotificationException(String message) {
        super(message);
    }

    public static class NotificationNotFoundException extends NotificationException {
        public NotificationNotFoundException(String message) {
            super(message);
        }
    }
}
