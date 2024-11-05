package ru.telproject.service.command;

import org.checkerframework.checker.units.qual.A;
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
import ru.telproject.exception.*;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.validator.RecordingValidator;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecordCommandTest {
    @Mock
    private TypeRecordingService typeRecordingService;
    @Mock
    private AppUserService appUserService;
    @Mock
    private RecordingUserRepository recordingUserRepository;
    @Spy
    private RecordingValidator recordingValidator;

    @InjectMocks
    private UpdateRecordCommand updateRecordCommand;

    @Mock
    private Message message;
    private AppUser appUser;
    private TypeRecording typeRecording;
    @BeforeEach
    void setUp() {
        when(message.getChatId())
                .thenReturn(123L);

        appUser = new AppUser();
        appUser.setId(1L);
        appUser.setTelegramUserId(123L);
        typeRecording = TypeRecording.builder()
                .typeName("маникюр")
                .id(1L)
                .build();
    }

    @Test
    @DisplayName("Успешное изменение записи первое сообщение изменение времени")
    void executeFirstMessageSuccessful() {
        String text = "Измени время записи на маникюр сегодня с 14:00 на 15:00";
        RecordingUser recordingUser = RecordingUser.builder().recordingTime(LocalDateTime.now().withHour(14).withMinute(0))
                .typeRecording(typeRecording)
                .appUser(appUser)
                .build();
        when(message.getText()).thenReturn(text);
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of(typeRecording));
        when(recordingUserRepository.findRecordsByTimeTypeIdUserID(any(LocalDateTime.class),
                anyLong(),
                anyLong()))
                .thenReturn(Optional.of(recordingUser));
        SendMessage sendMessage = updateRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertEquals(LocalDateTime.now().withHour(15).withMinute(0), getRecordingUserInMap(123L).getRecordingTime());
        verifySuccessfulFirstMessage(sendMessage);
    }
    @Test
    @DisplayName("Успешное изменение записи первое сообщение изменение времени на специальную дату")
    void executeFirstMessageSuccessfulWithSpecificDate() {
        String text = "Измени время записи на маникюр 25 декабря с 14:00 на 29 декабря 15:00";
        RecordingUser recordingUser = RecordingUser.builder().recordingTime(LocalDateTime.now().withHour(14).withMinute(0))
                .typeRecording(typeRecording)
                .appUser(appUser)
                .build();
        when(message.getText()).thenReturn(text);
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of(typeRecording));
        when(recordingUserRepository.findRecordsByTimeTypeIdUserID(any(LocalDateTime.class),
                anyLong(),
                anyLong()))
                .thenReturn(Optional.of(recordingUser));
        SendMessage sendMessage = updateRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertEquals(LocalDateTime.now()
                .withMonth(12).withDayOfMonth(29).withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                getRecordingUserInMap(123L).getRecordingTime());
        verifySuccessfulFirstMessage(sendMessage);
    }

    @Test
    @DisplayName("Обработка первого сообщения при отсутствии даты")
    void  executeFirstMessageWithNoLocalDate(){
        String text = "Измени время записи на маникюр с 14:00 на 15:00";
        TypeRecording typeRecording = TypeRecording.builder()
                .typeName("маникюр")
                .id(1L)
                .build();
        when(message.getText()).thenReturn(text);
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of(typeRecording));
        assertThrows(InvalidRecordingTimeException.class, () ->
                updateRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Обработка первого сообщения при отсутвии времени")
    void  executeFirstMessageWithNoLocalTime(){
        String text = "Измени время записи на маникюр";
        when(message.getText()).thenReturn(text);
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of(typeRecording));
        assertThrows(InvalidRecordingTimeException.class, () ->
                updateRecordCommand.executeFirstMessage(message));
    }
    @Test
    @DisplayName("Обработка первого сообщения c пустым пользователем")
    void  executeFirstMessageWithNoUser(){
        String text = "Измени время записи на маникюр";
        when(message.getText()).thenReturn(text);
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () ->
                updateRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Ошибка выполнения следующего сообщения при несуществующем типе услуги")
    void findTypeRecordingsNoMatchingTypeShouldThrowException() {
        when(message.getText()).thenReturn("Измени время записи на педикюр 25 декабря с 14:00 на 29 декабря 15:00");
        when(appUserService.findAppUserByTelegramId(123L)).thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(1L)).thenReturn(Collections.emptyList());

        assertThrows(InvalidRecordTypeException.class, () -> updateRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Обработка обновления записи если пользователь отправил описание")
    void executeNextMessageWithDescriptionSuccessfulSave() {
        String description = "Телефон: +7999123456";
        when(message.getText()).thenReturn(description);
        RecordingUser recordingUser = createRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = updateRecordCommand.executeNextMessage(message);
        verifySuccessfulNextMessage(result, true);
        verify(recordingUserRepository).save(argThat(ru -> ru.getDescription().equals(description)));
    }

    @Test
    @DisplayName("Обратка обновления записи если пользователь отказался менять описание")
    void executeNextMessageWithoutDescriptionSuccessfulSave() {
        String description = "нет";
        when(message.getText()).thenReturn(description);
        RecordingUser recordingUser = createRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = updateRecordCommand.executeNextMessage(message);
        verifySuccessfulNextMessage(result, false);
        verify(recordingUserRepository).save(argThat(ru -> ru.getDescription() == null));
    }

    @Test
    @DisplayName("Ошибка при обновлении сообщений при отсутсвии временных данных")
    void executeNextMessageWithoutTemporaryData(){
        when(message.getText()).thenReturn("тест");
        assertThrows(NotFoundTemporaryDateUser.class, () -> updateRecordCommand.executeNextMessage(message));
    }

    @Test
    @DisplayName("Проверка очистки временных данных после обработки следующего сообщения")
    void executeNextMessageClearTemporaryData() {
        when(message.getText()).thenReturn("нет");
        RecordingUser recordingUser = createRecordingUser();
        setRecordingUserInMap(123L, recordingUser);

        Pair<SendMessage, String> result = updateRecordCommand.executeNextMessage(message);

        verifySuccessfulNextMessage(result, false);
        verify(recordingUserRepository).save(argThat(ru ->
                ru.getDescription() == null));
        assertNull(getRecordingUserInMap(123L));

    }

    private void verifySuccessfulFirstMessage(SendMessage result) {
        assertNotNull(result);
        assertTrue(result.getText().contains("Желаете изменить описание к записи?\nТекущее описание:"));
    }

    private void setRecordingUserInMap(Long userId, RecordingUser recordingUser) {
        try {
            Field mapRecordField = UpdateRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(updateRecordCommand);
            mapRecord.put(userId, recordingUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RecordingUser getRecordingUserInMap(Long chatId){
        try {
            Field mapRecordField = UpdateRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(updateRecordCommand);
            return mapRecord.get(chatId);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }

    private RecordingUser createRecordingUser() {
        RecordingUser recordingUser = new RecordingUser();
        recordingUser.setRecordingTime(LocalDateTime.now());
        recordingUser.setAppUser(appUser);
        recordingUser.setTypeRecording(typeRecording);
        return recordingUser;
    }

    private void verifySuccessfulNextMessage(Pair<SendMessage, String> result, boolean hasDescription) {
        assertNotNull(result);
        assertTrue(result.getFirst().getText().contains("Изменил запись на маникюр"));
        assertEquals("classpath:sticker/saveit.webm", result.getSecond());
        if (hasDescription) {
            assertTrue(result.getFirst().getText().contains("Описание к записи"));
        } else {
            assertFalse(result.getFirst().getText().contains("Описание к записи"));
        }
    }
}