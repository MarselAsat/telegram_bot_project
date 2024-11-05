package ru.telproject.exception_handle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telproject.exception.*;

import java.time.format.DateTimeParseException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public SendMessage handleUserNotFoundException(UserNotFoundException ex){
        log.warn("User not found: {}", ex.getMessage());
        return SendMessage.builder()
                .text("Пользователь не найден. Пожалуйста, начните с команды /start")
                .build();
    }

    @ExceptionHandler(TypeRecordingNotFoundException.class)
    public SendMessage handleTypeRecordNotFoundException(TypeRecordingNotFoundException ex){
        log.warn("Invalid datetime format: {}", ex.getMessage());
        return SendMessage.builder()
                .text("Неверный формат даты/времени. Пример: 'запись на массаж завтра в 15:00'")
                .build();
    }

    @ExceptionHandler(InvalidRecordingTimeException.class)
    public SendMessage handleNotValidRecordTimeException(InvalidRecordingTimeException ex){
        log.warn("Invalid time to record {}", ex.getMessage());
        return SendMessage.builder()
                .text("Не возможно создать запись на уже прошедшее время")
                .build();
    }

    @ExceptionHandler(NotFoundStickerException.class)
    public void handleNotFoundStickerException(NotFoundStickerException ex){
        log.warn("Not found sticker exception");
    }
    @ExceptionHandler(NotFoundTemporaryDateUser.class)
    public void handleNotFoundTemporaryDateUserException(NotFoundStickerException ex){
        log.warn("Service not found temporary date for User");
    }

    @ExceptionHandler(DateTimeParseException.class)
    public void handlerDateTimeParseException(DateTimeParseException ex){
        log.warn("Date/time parse exception. ex = " + ex.getMessage());
    }
}
