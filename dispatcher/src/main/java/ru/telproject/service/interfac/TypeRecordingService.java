package ru.telproject.service.interfac;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.entity.TypeRecording;

import java.util.List;
import java.util.Optional;

public interface TypeRecordingService {
    List<TypeRecording> findByTypeNameIgnoreCase(String typeName, Long appUserTelegramId);
    TypeRecording createType(String typeName, Double coast, Long appUserId);

    List<TypeRecording> findAllType(Long appUserId);

    void deleteType(Long typeId);
}
