package ru.telproject.exception;

public class NotFoundTemporaryDateUser extends RuntimeException {
    public NotFoundTemporaryDateUser(String message) {
        super(message);
    }
}
