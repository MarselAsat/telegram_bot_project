package ru.telproject.validator;

import org.springframework.stereotype.Component;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.InvalidRecordTypeException;
import ru.telproject.exception.InvalidRecordingTimeException;

import java.time.LocalDateTime;

@Component
public class RecordingValidator {
    public void validatorRecordingTime(LocalDateTime recordingTime){
        if (recordingTime.isBefore(LocalDateTime.now())){
            throw new InvalidRecordingTimeException("Нельзя создать записи на прошедшее время");
        }
    }

    public void validateTypeRecording(TypeRecording typeRecording){
        if (typeRecording == null) {
            throw new InvalidRecordTypeException("Тип записи не найден");
        }
    }
}
