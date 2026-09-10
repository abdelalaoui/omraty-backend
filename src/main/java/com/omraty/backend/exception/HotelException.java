package com.omraty.backend.exception;

public class HotelException extends RuntimeException {

    HotelException(String message) {
        super(message);
    }

    public static class InvalidHotelRequestException extends HotelException {
        public InvalidHotelRequestException(String message) {
            super(message);
        }
    }

    public static class HotelNotFoundException extends HotelException {
        public HotelNotFoundException(String message) {
            super(message);
        }
    }
}
