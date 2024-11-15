package ru.telproject.service.interfac;

import ru.telproject.entity.RecordingUser;

import java.time.LocalDate;
import java.util.List;

public interface RecordingUserService {
    List<RecordingUser> findUserRecordingsByDate(Long appUserTelegramId, String date);
}
