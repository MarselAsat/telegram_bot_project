package ru.telproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.InvalidRecordingTimeException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.validator.RecordingValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordingServiceTest {
    @Mock
    private AppUserService appUserService;
    @Mock
    private TypeRecordingService typeRecordingService;
    @Spy
    private RecordingValidator recordingValidator;

    @Mock
    private Message message;
    @InjectMocks
    private RecordingService recordingService;

    @BeforeEach
    void setUp(){
        when(message.getChatId())
                .thenReturn(123L);

    }

    @Test
    void createRecording() {
        Long chatId = 123L;
        LocalDateTime recordingTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0);
        String recordTypeName = "массаж";
        AppUser user = AppUser.builder()
                .telegramUserId(chatId).build();
        TypeRecording typeRecording = TypeRecording.builder().typeName(recordTypeName).build();
        when(appUserService.findAppUserByTelegramId(chatId))
                .thenReturn(Optional.of(user));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(recordTypeName, user.getId()))
                .thenReturn(List.of(typeRecording));
        when(typeRecordingService.findAllByAppUserId(user.getId()))
                .thenReturn(List.of(typeRecording));
        when(message.getText())
                .thenReturn("создай запись на массаж 25 октября в 15:00");

        RecordingUser result = recordingService.createRecording(message, recordingTime);
        assertNotNull(result);
        assertEquals(typeRecording, result.getTypeRecording());
        assertEquals(chatId, result.getAppUser().getTelegramUserId());
    }

    @Test
    void createRecordingThrowsException(){
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        assertThrows(InvalidRecordingTimeException.class, () ->
                recordingService.createRecording(message, pastTime));
    }
}