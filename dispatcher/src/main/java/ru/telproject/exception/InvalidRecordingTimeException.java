package ru.telproject.exception;

public class InvalidRecordingTimeException extends RuntimeException {
    public InvalidRecordingTimeException(String message) {
        super(message);
    }
}
