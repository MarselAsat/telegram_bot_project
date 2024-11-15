package ru.telproject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.command.CreateRecordCommand;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRecordCommandTest {
    @Mock
    private RecordingUserRepository recordingUserRepository;
    @Mock
    private TypeRecordingService typeRecordingService;
    @Mock
    private AppUserService appUserService;
    @InjectMocks
    private CreateRecordCommand createRecordCommand;

    private AppUser testAppUser;
    private TypeRecording testTypeRecording;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testAppUser = new AppUser();
        testAppUser.setId(1L);
        testAppUser.setTelegramUserId(123L);

        testTypeRecording = new TypeRecording();
        testTypeRecording.setId(1L);
        testTypeRecording.setTypeName("массаж");
        testTypeRecording.setAppUser(testAppUser);

        testMessage = mock(Message.class);
        when(testMessage.getChatId()).thenReturn(123L);
    }

    @Test
    void executeFirstMessage_SuccessfulCreation_WithTodayDate() throws IOException {
        // Подготовка
        when(testMessage.getText()).thenReturn("Запись на массаж сегодня в 15:00");
        setupCommonMocks();

        // Выполнение
        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        // Проверка
        verifySuccessfulFirstMessage(result);
    }

    @Test
    void executeFirstMessage_SuccessfulCreation_WithTomorrowDate() throws IOException {
        // Подготовка
        when(testMessage.getText()).thenReturn("Запись на массаж завтра в 15:00");
        setupCommonMocks();

        // Выполнение
        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        // Проверка
        verifySuccessfulFirstMessage(result);
    }

    @Test
    void executeFirstMessage_SuccessfulCreation_WithSpecificDate() throws IOException {
        // Подготовка
        when(testMessage.getText()).thenReturn("Запись на массаж 25.12.2024 в 15:00");
        setupCommonMocks();

        // Выполнение
        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        // Проверка
        verifySuccessfulFirstMessage(result);
    }

    @Test
    void executeFirstMessage_InvalidDateTime_ShouldThrowException() {
        // Подготовка
        when(testMessage.getText()).thenReturn("Запись на массаж в некорректное время");
        setupCommonMocks();

        // Проверка
        assertThrows(RuntimeException.class, () -> createRecordCommand.executeFirstMessage(testMessage));
    }

    @Test
    void executeFirstMessage_MultipleTypeRecordings_ShouldUseFirst() throws IOException {
        // Подготовка
        TypeRecording secondType = new TypeRecording();
        secondType.setTypeName("массаж спины");

        when(testMessage.getText()).thenReturn("Запись на массаж завтра в 15:00");
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(testAppUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(Arrays.asList(testTypeRecording, secondType));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(anyString(), anyLong()))
                .thenReturn(Arrays.asList(testTypeRecording, secondType));

        // Выполнение
        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        // Проверка
        verifySuccessfulFirstMessage(result);
    }

    @Test
    void executeNextMessage_WithDescription_SuccessfulSave() {
        // Подготовка
        String description = "Телефон: +7999123456";
        when(testMessage.getText()).thenReturn(description);
        RecordingUser recordingUser = createTestRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        // Выполнение
        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        // Проверка
        verifySuccessfulNextMessage(result, true);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription().equals(description)));
    }

    @Test
    void executeNextMessage_WithoutDescription_SuccessfulSave() {
        // Подготовка
        when(testMessage.getText()).thenReturn("нет");
        RecordingUser recordingUser = createTestRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        // Выполнение
        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        // Проверка
        verifySuccessfulNextMessage(result, false);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription() == null));
    }

    @Test
    void executeNextMessage_WithEmptyMap_ShouldHandleGracefully() {
        // Подготовка
        when(testMessage.getText()).thenReturn("тест");

        // Выполнение
        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        // Проверка
        assertNotNull(result);
        verify(recordingUserRepository).save(any(RecordingUser.class));
    }

    @Test
    void findTypeRecordings_NoMatchingType_ShouldThrowException() {
        // Подготовка
        when(testMessage.getText()).thenReturn("Запись на несуществующую услугу завтра в 15:00");
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(testAppUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(Collections.emptyList());

        // Проверка
        assertThrows(RuntimeException.class, () -> createRecordCommand.executeFirstMessage(testMessage));
    }

    // Вспомогательные методы
    private void setupCommonMocks() {
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(testAppUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(List.of(testTypeRecording));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(anyString(), anyLong()))
                .thenReturn(List.of(testTypeRecording));
    }

    private void verifySuccessfulFirstMessage(SendMessage result) {
        assertNotNull(result);
        assertTrue(result.getText().contains("Если вы желаете добавить описание к записи"));
    }

    private void verifySuccessfulNextMessage(Pair<SendMessage, String> result, boolean hasDescription) {
        assertNotNull(result);
        assertTrue(result.getFirst().getText().contains("Создал запись на массаж"));
        assertEquals("classpath:sticker/saveit.webm", result.getSecond());
        if (hasDescription) {
            assertTrue(result.getFirst().getText().contains("Описание к записи"));
        } else {
            assertFalse(result.getFirst().getText().contains("Описание к записи"));
        }
    }

    private RecordingUser createTestRecordingUser() {
        return RecordingUser.builder()
                .typeRecording(testTypeRecording)
                .recordingTime(LocalDateTime.now())
                .appUser(testAppUser)
                .build();
    }

    private void setRecordingUserInMap(Long userId, RecordingUser recordingUser) {
        try {
            Field mapRecordField = CreateRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(createRecordCommand);
            mapRecord.put(userId, recordingUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
