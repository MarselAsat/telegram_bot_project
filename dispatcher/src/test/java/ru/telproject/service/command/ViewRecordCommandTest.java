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
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewRecordCommandTest {
    @Mock
    private RecordingUserRepository recordingUserRepository;
    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private ViewRecordCommand viewRecordCommand;

    @Mock
    private Message message;
    private AppUser appUser;
    @BeforeEach
    void setUp() {
        when(message.getChatId())
                .thenReturn(123L);
        appUser = new AppUser();
        appUser.setId(1L);
        appUser.setTelegramUserId(123L);
    }

    @Test
    @DisplayName("Первое сообщение: успешное отображение записей пользователей на сегодня")
    void executeFirstMessageSuccessfulToday() {
        LocalDateTime dateTime = LocalDateTime.now();
        RecordingUser firstRecord = createRecord(dateTime.withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "макияж", "просила вечерний");
        RecordingUser secondRecord = createRecord(dateTime.withHour(18).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "стрижка", null);
        when(message.getText())
                .thenReturn("Покажи все записи на сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(recordingUserRepository.findByAppUserIdAndTimeBetween(appUser.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59)))
                .thenReturn(List.of(firstRecord,secondRecord));
        SendMessage sendMessage = viewRecordCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("Ваши записи на " + LocalDate.now()));
        assertTrue(sendMessage.getText().contains("Запись в - " +
                firstRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + firstRecord.getTypeRecording().getTypeName() + "; комментарии - " + firstRecord.getDescription()));
        assertTrue(sendMessage.getText().contains("Запись в - " +
                secondRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + secondRecord.getTypeRecording().getTypeName()));
    }
    @Test
    @DisplayName("Первое сообщение: успешное отображение записей пользователей на конкретную дату")
    void executeFirstMessageSuccessfulWithSpecificDate() {
        LocalDateTime dateTime = LocalDateTime.now().withMonth(11).withDayOfMonth(25);
        RecordingUser firstRecord = createRecord(dateTime.withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "макияж", "просила вечерний");
        RecordingUser secondRecord = createRecord(dateTime.withHour(18).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "стрижка", null);
        when(message.getText())
                .thenReturn("Покажи все записи на 25 ноября");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(recordingUserRepository.findByAppUserIdAndTimeBetween(appUser.getId(),
                LocalDate.now().withMonth(11).withDayOfMonth(25).atStartOfDay(),
                LocalDate.now().withMonth(11).withDayOfMonth(25).atTime(23, 59)))
                .thenReturn(List.of(firstRecord,secondRecord));
        SendMessage sendMessage = viewRecordCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("Ваши записи на " + LocalDate.now()));
        assertTrue(sendMessage.getText().contains("Запись в - " +
                firstRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + firstRecord.getTypeRecording().getTypeName() + "; комментарии - " + firstRecord.getDescription()));
        assertTrue(sendMessage.getText().contains("Запись в - " +
                secondRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + secondRecord.getTypeRecording().getTypeName()));
    }

    @Test
    @DisplayName("Первое сообщение: запрос пользователем записей без конкретной даты")
    void executeFirstMessageSuccessfulWithNoDate() {
        when(message.getText())
                .thenReturn("Покажи все записи");

        SendMessage sendMessage = viewRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText().contains("На какую дату вы хотите посмотреть записи?"));
    }

    @Test
    @DisplayName("Первое сообщение: ошибка при отсутсвии пользователя")
    void executeFirstMessageWithNoUser() {
        when(message.getText())
                .thenReturn("Покажи все записи на сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> viewRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Первое сообщение: отсутствие записей")
    void executeFirstMessageWithNoRecord() {
        when(message.getText())
                .thenReturn("Покажи все записи на сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(recordingUserRepository.findByAppUserIdAndTimeBetween(appUser.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59)))
                .thenReturn(List.of());
        SendMessage sendMessage = viewRecordCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("У вас нет записей на " + LocalDate.now()));
    }

    @Test
    @DisplayName("Второе сообщение: отображение записей пользователей при отправке пользователем даты")
    void executeNextMessageSuccessful() {
        LocalDateTime dateTime = LocalDateTime.now();
        RecordingUser firstRecord = createRecord(dateTime.withHour(15).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "макияж", "просила вечерний");
        RecordingUser secondRecord = createRecord(dateTime.withHour(18).withMinute(0).truncatedTo(ChronoUnit.MINUTES),
                "стрижка", null);
        when(message.getText())
                .thenReturn("сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(recordingUserRepository.findByAppUserIdAndTimeBetween(appUser.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59)))
                .thenReturn(List.of(firstRecord,secondRecord));
        Pair<SendMessage, String> pair = viewRecordCommand.executeNextMessage(message);
        assertTrue(pair.getFirst().getText().contains("Запись в - " +
                firstRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + firstRecord.getTypeRecording().getTypeName() + "; комментарии - " + firstRecord.getDescription()));
        assertTrue(pair.getFirst().getText().contains("Запись в - " +
                secondRecord.getRecordingTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                ";услуга - " + secondRecord.getTypeRecording().getTypeName()));
    }
    @Test
    @DisplayName("Второе сообщение: ошибка при отсутсвии пользователя")
    void executeNextMessageWithNoUser() {
        when(message.getText())
                .thenReturn("сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> viewRecordCommand.executeNextMessage(message));
    }

    @Test
    @DisplayName("Второе сообщение: некорректная дата")
    void executeNextMessageWithIncorrectDate() {
        when(message.getText())
                .thenReturn("dfgdfgh");
        assertThrows(DateTimeParseException.class, () -> viewRecordCommand.executeNextMessage(message));
    }


    @Test
    @DisplayName("Второе сообщение: отсутствие записей")
    void executeNextMessageWithNoRecord() {
        when(message.getText())
                .thenReturn("Покажи все записи на сегодня");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(recordingUserRepository.findByAppUserIdAndTimeBetween(appUser.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59)))
                .thenReturn(List.of());
        SendMessage sendMessage = viewRecordCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("У вас нет записей на " + LocalDate.now()));
    }

    private RecordingUser createRecord(LocalDateTime recordTime, String typeName, String description){
        return RecordingUser.builder()
                .recordingTime(recordTime)
                .typeRecording(TypeRecording.builder().typeName(typeName).build())
                .appUser(appUser)
                .description(description != null ? description : null)
                .build();
    }
}