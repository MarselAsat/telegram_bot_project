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
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTypeRecordCommandTest {

    @Mock
    private ConcurrentHashMap<Long, String> userDate;
    @Mock
    private TypeRecordingService typeRecordingService;
    @Mock
    private AppUserService appUserService;
    @InjectMocks
    private CreateTypeRecordCommand createTypeRecordCommand;

    private Message message;
    private AppUser appUser;
    @BeforeEach
    void setUp(){
        message = mock(Message.class);
        when(message.getChatId()).thenReturn(123L);

        appUser = new AppUser();
        appUser.setId(1L);
        appUser.setTelegramUserId(123L);
    }

    @Test
    @DisplayName("Успешное создание первого сообщения для создания типа услуги")
    void executeFirstMessagesSuccess() {
        when(message.getText())
                .thenReturn("создай услугу маникюр");
        SendMessage sendMessage = createTypeRecordCommand.executeFirstMessage(message);

        assertNotNull(sendMessage);
        assertEquals("Какая цена будет у услуги маникюр?", sendMessage.getText());
        assertEquals("маникюр", createTypeRecordCommand.getUserData().get(123L));
        assertTrue(createTypeRecordCommand.getUserData().containsKey(123L));
    }
    @Test
    @DisplayName("Успешное создание типа услуги ")
    void executeNextMessageSuccess() {
        when(message.getText())
                .thenReturn("3000");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        TypeRecording typeRecording = new TypeRecording();
        typeRecording.setTypeName("маникюр");
        typeRecording.setAppUser(appUser);
        setUserDate(123L, typeRecording.getTypeName());


        Pair<SendMessage, String> pair = createTypeRecordCommand.executeNextMessage(message);

        assertNotNull(pair);
        assertEquals("classpath:sticker/yes-sir.webm", pair.getSecond());
        assertTrue(pair.getFirst().getText().contains("Создал услугу: маникюр \n цена услуги: 3000.0"));

        verify(typeRecordingService).saveTypeRecord(argThat(type ->
                type.getTypeName().equals("маникюр") &&
                type.getTypeCoast() == 3000.0 &&
                type.getAppUser().getId().equals(appUser.getId())));
    }

    @Test
    @DisplayName("Ошибка при обработке второго сообщения при отсутствии пользователя")
    void executeNextMessagesExceptionUser() {
        setUserDate(123L, "маникюр");
        when(message.getText())
                .thenReturn("3000");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> createTypeRecordCommand.executeNextMessage(message));
        assertEquals("Пользователь не найден. telegramUSerID: 123", exception.getMessage());
    }
    @Test
    @DisplayName("Проверка на существование типа записи передаваемого пользователем")
    void executeNextMessagesExceptionTypeRecord() {
        setUserDate(123L, "маникюр");
        when(message.getText())
                .thenReturn("3000");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId("маникюр", appUser.getId()))
                .thenReturn(List.of(TypeRecording.builder().typeName("маникюр").build()));
        Pair<SendMessage, String> pair = createTypeRecordCommand.executeNextMessage(message);
        assertNotNull(pair);
        assertEquals("У вас уже есть такая услуга: маникюр \nНе получится сохранить",pair.getFirst().getText());
        assertTrue(pair.getSecond().contains("classpath"));
    }

    @Test
    @DisplayName("Ошибка при отсуствии временных данных")
    void executeNextMessagesNoTemporaryData() {
        when(message.getText())
                .thenReturn("3000");
        assertThrows(NotFoundTemporaryDateUser.class, () -> createTypeRecordCommand.executeNextMessage(message));
    }

    @Test
    @DisplayName("Проверка очистки временных данных после сохранения")
    void executeNextMessageClearTemporaryData(){
        when(message.getText())
                .thenReturn("3000");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        setUserDate(123L, "маникюр");
        TypeRecording typeRecording = TypeRecording.builder().typeName("маникюр")
                .appUser(appUser)
                .typeCoast(3000d)
                .build();
        createTypeRecordCommand.executeNextMessage(message);
        assertNull(getUserDate(123L));
    }

    private String getUserDate(Long chatId){
        try {
            Field userData = CreateTypeRecordCommand.class.getDeclaredField("userData");
            userData.setAccessible(true);
            ConcurrentHashMap<Long, String> map = (ConcurrentHashMap<Long, String>) userData.get(createTypeRecordCommand);
            return map.get(chatId);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }

    private void setUserDate(Long chatId, String text){
        try {
            Field userData = CreateTypeRecordCommand.class.getDeclaredField("userData");
            userData.setAccessible(true);
            ConcurrentHashMap<Long, String> map = (ConcurrentHashMap<Long, String>) userData.get(createTypeRecordCommand);
            map.put(chatId, text);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }
}