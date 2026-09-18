package com.omraty.backend.exception;

public class AppSettingException extends RuntimeException {

    AppSettingException(String message) {
        super(message);
    }

    public static class AppSettingNotFoundException extends AppSettingException {
        public AppSettingNotFoundException(String message) {
            super(message);
        }
    }
}
