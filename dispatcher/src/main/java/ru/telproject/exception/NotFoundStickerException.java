package ru.telproject.exception;

public class NotFoundStickerException extends RuntimeException{
    public NotFoundStickerException(String message){
        super(message);
    }
}
