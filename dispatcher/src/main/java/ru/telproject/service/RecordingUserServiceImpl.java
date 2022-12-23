package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.interfac.AppUserService;
import ru.telproject.service.interfac.RecordingUserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecordingUserServiceImpl implements RecordingUserService {

    private final AppUserService appUserService;
    private final RecordingUserRepository recordingUserRepository;

    @Override
    public List<RecordingUser> findUserRecordingsByDate(Long appUserTelegramId, String date) {

        Optional<AppUser> maybeUser = appUserService.findByTelegramId(appUserTelegramId);
        LocalDate recordingDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        Long userId = maybeUser.get().getId();
        List<RecordingUser> userRecordings = recordingUserRepository.findByAppUserIdAndRecordingTime(userId, recordingDate);

        userRecordings.forEach(o -> o.getTypeRecording().getTypeName());
        return userRecordings;
    }
}
