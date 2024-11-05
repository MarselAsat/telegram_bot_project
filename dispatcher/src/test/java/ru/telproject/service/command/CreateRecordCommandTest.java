package ru.telproject.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.IllegalDateException;
import ru.telproject.exception.InvalidRecordTypeException;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.TypeRecordingNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.RecordingService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.command.CreateRecordCommand;
import ru.telproject.validator.RecordingValidator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

    @Spy
    private RecordingValidator recordingValidator;


    private RecordingService recordingService;

    private CreateRecordCommand createRecordCommand;

    private AppUser testAppUser;
    private TypeRecording testTypeRecording;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        recordingService = new RecordingService(typeRecordingService,
                appUserService,
                recordingValidator);
        createRecordCommand = new CreateRecordCommand(recordingUserRepository,
                recordingService,
                recordingValidator);
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
    @DisplayName("Создание записи на сегодня первое сообщение")
    void executeFirstMessageSuccessfulCreationWithTodayDate(){
        when(testMessage.getText()).thenReturn("Запись на массаж сегодня в 22:00");
        setupCommonMocks();

        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        verifySuccessfulFirstMessage(result);
    }

    @Test
    @DisplayName("Создание записи на завра первое сообщение")
    void executeFirstMessageSuccessfulCreationWithTomorrowDate(){
        when(testMessage.getText()).thenReturn("Запись на массаж завтра в 15:00");
        setupCommonMocks();

        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        verifySuccessfulFirstMessage(result);
    }

    @Test
    @DisplayName("Создание записи на определенную дату первое сообщение")
    void executeFirstMessageSuccessfulCreationWithSpecificDate(){
        when(testMessage.getText()).thenReturn("Запись на массаж 25 декабря в 15:00");
        setupCommonMocks();

        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        verifySuccessfulFirstMessage(result);
    }

    @Test
    @DisplayName("Ошибка некорректной даты первое сообщение")
    void executeFirstMessageInvalidDateTimeShouldThrowException() {
        when(testMessage.getText()).thenReturn("Запись на массаж в некорректное время");

        assertThrows(DateTimeParseException.class, () -> createRecordCommand.executeFirstMessage(testMessage));
    }

    @Test
    @DisplayName("Проверка получения первого типа записи, при существовании нескольких схожих типов, если указано не полное имя")
    void executeFirstMessageMultipleTypeRecordingsShouldUseFirst(){
        TypeRecording secondType = new TypeRecording();
        secondType.setTypeName("массаж спины");

        when(testMessage.getText()).thenReturn("Запись на массаж завтра в 15:00");
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(testAppUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(Arrays.asList(testTypeRecording, secondType));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(anyString(), anyLong()))
                .thenReturn(Arrays.asList(testTypeRecording, secondType));

        SendMessage result = createRecordCommand.executeFirstMessage(testMessage);

        verifySuccessfulFirstMessage(result);
    }

    @Test
    @DisplayName("Проверка сохранения записи при если пользователь отправил описание")
    void executeNextMessageWithDescriptionSuccessfulSave() {
        String description = "Телефон: +7999123456";
        when(testMessage.getText()).thenReturn(description);
        RecordingUser recordingUser = createTestRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        verifySuccessfulNextMessage(result, true);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription().equals(description)));
    }

    @Test
    @DisplayName("Проверка сохранения записи если пользователь отказался добавлять описание")
    void executeNextMessageWithoutDescriptionSuccessfulSave() {
        when(testMessage.getText()).thenReturn("нет");
        RecordingUser recordingUser = createTestRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        verifySuccessfulNextMessage(result, false);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription() == null));
    }

    @Test
    @DisplayName("Ошибка выполнения следующего сообщения при отсутствии временных данных")
    void executeNextMessageWithoutTemporaryData() {
        when(testMessage.getText()).thenReturn("тест");

        assertThrows(NotFoundTemporaryDateUser.class,() -> createRecordCommand.executeNextMessage(testMessage));
    }

    @Test
    @DisplayName("Ошибка выполнения следующего сообщения при несуществующем типе услуги")
    void findTypeRecordingsNoMatchingTypeShouldThrowException() {
        when(testMessage.getText()).thenReturn("Запись на несуществующую услугу завтра в 15:00");
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(testAppUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(Collections.emptyList());

        assertThrows(TypeRecordingNotFoundException.class, () -> createRecordCommand.executeFirstMessage(testMessage));
    }

    @Test
    @DisplayName("Проверка очистки временных данных после обработки следующего сообщения")
    void executeNextMessageClearTemporaryData() {
        when(testMessage.getText()).thenReturn("нет");
        RecordingUser recordingUser = createTestRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = createRecordCommand.executeNextMessage(testMessage);

        verifySuccessfulNextMessage(result, false);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription() == null));
        assertNull(getRecordingUserInMap(123L));

    }

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

    private RecordingUser getRecordingUserInMap(Long chatId){
        try {
            Field mapRecordField = CreateRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(createRecordCommand);
            return mapRecord.get(chatId);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }
}
