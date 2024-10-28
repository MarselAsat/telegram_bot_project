package ru.telproject.service.custom_interface;

import ru.telproject.entity.RecordingUser;

import java.util.List;

public interface RecordingUserService {
    List<RecordingUser> findUserRecordingsByDate(Long appUserTelegramId, String date);
}
