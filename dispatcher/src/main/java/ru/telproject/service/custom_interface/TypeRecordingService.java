package ru.telproject.service.custom_interface;

import ru.telproject.entity.TypeRecording;

import java.util.List;

public interface TypeRecordingService {
    List<TypeRecording> findByTypeNameIgnoreCase(String typeName, Long appUserTelegramId);
    TypeRecording createType(String typeName, Double coast, Long appUserId);

    List<TypeRecording> findAllType(Long appUserId);

    void deleteType(Long typeId);
}
