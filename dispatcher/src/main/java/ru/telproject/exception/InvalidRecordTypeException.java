package ru.telproject.exception;

public class InvalidRecordTypeException extends RuntimeException {
    public InvalidRecordTypeException(String message) {
        super(message);
    }
}
