package ru.telproject.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.TypeRecordingNotFoundException;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRecordCommandTest {
    @Mock
    private RecordingUserRepository recordingUserRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private TypeRecordingService typeRecordingService;

    @InjectMocks
    private DeleteRecordCommand deleteRecordCommand;

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

        typeRecording = new TypeRecording();
        typeRecording.setId(1L);
        typeRecording.setTypeName("маникюр");
        typeRecording.setAppUser(appUser);
    }

    @Test
    @DisplayName("Первое сообщение: успешная обработка удаления записи")
    void executeFirstMessageSuccessful() {
        when(message.getText())
                .thenReturn("Удали запись на услугу маникюр 16 октября в 15:00");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService
                .findByTypeNameIgnoreCaseAndAppUserId(typeRecording.getTypeName(), appUser.getId()))
                .thenReturn(List.of(typeRecording));
        LocalDateTime recordTime = LocalDateTime.now().withMonth(10)
                .withDayOfMonth(16).withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        RecordingUser recordingUser = createRecordingUser(
                recordTime);

        when(recordingUserRepository.findRecordsByTimeTypeIdUserID(recordTime,
                typeRecording.getId(), appUser.getId()))
                .thenReturn(Optional.of(recordingUser));
        SendMessage sendMessage = deleteRecordCommand.executeFirstMessage(message);
        verifySuccessfulFirstMessage(sendMessage);
    }

    @Test
    @DisplayName("Первое сообщение: удаление записи с отсуствием пользователя")
    void executeFirstMessageWithoutUser() {
        when(message.getText())
                .thenReturn("Удали запись на услугу маникюр 16 октября в 15:00");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> deleteRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Первое сообщение: удаление записи с несуществующим типом")
    void executeFirstMessageWithoutRecordType() {
        when(message.getText())
                .thenReturn("Удали запись на услугу маникюр 16 октября в 15:00");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService
                .findByTypeNameIgnoreCaseAndAppUserId(typeRecording.getTypeName(), appUser.getId()))
                .thenReturn(List.of());

        assertThrows(TypeRecordingNotFoundException.class, () -> deleteRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Первое сообщение: удаление записи с некорректной датой")
    void executeFirstMessageWithIncorrectDate() {
        when(message.getText())
                .thenReturn("Удали запись на услугу маникюр");

        assertThrows(DateTimeParseException.class, () -> deleteRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Второе сообщение: успешное удаление записи если пользователь согласился")
    void executeNextMessageSuccessful() {
        LocalDateTime recordTime = LocalDateTime.now().withMonth(10)
                .withDayOfMonth(16).withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        RecordingUser recordingUser = createRecordingUser(
                recordTime);
        setRecordingUserInMap(123L, recordingUser);
        when(message.getText()).thenReturn("да");

        Pair<SendMessage, String> pair = deleteRecordCommand.executeNextMessage(message);

        verifySuccessfulNextMessage(pair.getFirst());
        assertTrue(pair.getSecond().contains("classpath:sticker/"));
        verify(recordingUserRepository).delete(recordingUser);
    }
    @Test
    @DisplayName("Второе сообщение: если пользователь отказался")
    void executeNextMessageWithResponseNo() {
        LocalDateTime recordTime = LocalDateTime.now().withMonth(10)
                .withDayOfMonth(16).withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        RecordingUser recordingUser = createRecordingUser(
                recordTime);
        setRecordingUserInMap(123L, recordingUser);
        when(message.getText()).thenReturn("нет");

        Pair<SendMessage, String> pair = deleteRecordCommand.executeNextMessage(message);

        assertNotNull(pair.getFirst());
        assertEquals("Отменил операцию удаления записи на маникюр", pair.getFirst().getText());
        assertTrue(pair.getSecond().contains("classpath:sticker/"));
    }
    @Test
    @DisplayName("Второе сообщение: ошибка отсутствия временных данных")
    void executeNextMessageWirthNoTemporaryDate() {
        when(message.getText()).thenReturn("да");

        assertThrows(NotFoundTemporaryDateUser.class, () ->deleteRecordCommand.executeNextMessage(message));
    }

    @Test
    @DisplayName("Второе сообщение: проверка очистки временных данных после успешного удаления")
    void executeNextCheckRemoveTemporaryDate() {
        LocalDateTime recordTime = LocalDateTime.now().withMonth(10)
                .withDayOfMonth(16).withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        RecordingUser recordingUser = createRecordingUser(
                recordTime);
        setRecordingUserInMap(123L, recordingUser);
        when(message.getText()).thenReturn("да");

        Pair<SendMessage, String> pair = deleteRecordCommand.executeNextMessage(message);

        assertNull(getRecordingUserInMap(123L));
    }





    private void verifySuccessfulFirstMessage(SendMessage result) {
        assertNotNull(result);
        assertTrue(result.getText().contains("Желаете удалить запись на услугу маникюр?"));
    }
    private void verifySuccessfulNextMessage(SendMessage result) {
        assertNotNull(result);
        assertTrue(result.getText()
                .contains("Запись на маникюр " + LocalDateTime.now().getYear() + "-16-10 15:00. Успешно удалена"));
    }
    private RecordingUser createRecordingUser(LocalDateTime localDateTime) {
        RecordingUser recordingUser = new RecordingUser();
        recordingUser.setRecordingTime(localDateTime);
        recordingUser.setAppUser(appUser);
        recordingUser.setTypeRecording(typeRecording);
        return recordingUser;
    }
    private void setRecordingUserInMap(Long userId, RecordingUser recordingUser) {
        try {
            Field mapRecordField = DeleteRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(deleteRecordCommand);
            mapRecord.put(userId, recordingUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RecordingUser getRecordingUserInMap(Long chatId){
        try {
            Field mapRecordField = DeleteRecordCommand.class.getDeclaredField("mapRecord");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, RecordingUser> mapRecord =
                    (ConcurrentHashMap<Long, RecordingUser>) mapRecordField.get(deleteRecordCommand);
            return mapRecord.get(chatId);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }
}