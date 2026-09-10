package com.omraty.backend.exception;

public class RoomException extends RuntimeException {

    RoomException(String message) {
        super(message);
    }

    public static class InvalidRoomTypeException extends RoomException {
        public InvalidRoomTypeException(String message) {
            super(message);
        }
    }

    public static class GroupSizeExceededException extends RoomException {
        public GroupSizeExceededException(String message) {
            super(message);
        }
    }
}
