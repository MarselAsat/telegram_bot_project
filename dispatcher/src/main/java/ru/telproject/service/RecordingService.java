package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.TypeRecordingNotFoundException;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.utils.StemmingUtils;
import ru.telproject.utils.TextFinderUtils;
import ru.telproject.validator.RecordingValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecordingService {
    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    private final RecordingValidator recordingValidator;

    @SneakyThrows
    public RecordingUser createRecording(Message message, LocalDateTime recordingTime){
        log.info("Creating new recording. userid={}, recordingTime={}",
                message.getChatId(),
                recordingTime);
        recordingValidator.validatorRecordingTime(recordingTime);
        AppUser appUser = appUserService.findAppUserByTelegramId(message.getChatId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        TypeRecording typeRecording = findTypeRecordings(appUser, message.getText());
        recordingValidator.validateTypeRecording(typeRecording);
        return RecordingUser.builder()
                        .appUser(appUser)
                        .typeRecording(typeRecording)
                        .recordingTime(recordingTime)
                .build();
    }

    private TypeRecording findTypeRecordings(AppUser appUser, String messageText) throws IOException {
        List<TypeRecording> allByAppUserId = typeRecordingService.findAllByAppUserId(appUser.getId());
        List<String> typeNames = allByAppUserId.stream()
                .map(typeRecording -> typeRecording.getTypeName()).collect(Collectors.toList());
        List<String> stemmedWords = StemmingUtils.stemWords(typeNames);
        String regexTypeNames = stemmedWords.stream().collect(Collectors.joining("([аеиуой])?|"));
        String typeName = "";
        Matcher matcher = TextFinderUtils.findRecordOnText("(" + regexTypeNames + "([аеиуой])?)\\s+", messageText);
        while (matcher.find()) {
            typeName = matcher.group(1);
        }
        String filterValue = StemmingUtils.stemWords(List.of(typeName)).stream().findFirst().orElse("null");
        typeName = typeNames.stream().filter(t -> t.contains(filterValue)).findFirst()
                .orElseThrow(() -> new TypeRecordingNotFoundException(String.format("Услуга: %s не найдена", filterValue)));
        TypeRecording typeRecordings = typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(typeName,
                appUser.getId()).get(0);
        return typeRecordings;
    }
}
