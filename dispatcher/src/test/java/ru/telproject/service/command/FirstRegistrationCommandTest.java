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
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.service.AppUserService;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstRegistrationCommandTest {
    @Mock
    private AppUserService appUserService;

    @Mock
    private Message message;
    @InjectMocks
    private FirstRegistrationCommand firstRegistrationCommand;

    private AppUser appUser;
    @BeforeEach
    void setUp() {
        when(message.getChatId())
                .thenReturn(123L);
        appUser = new AppUser();
        appUser.setTelegramUserId(123L);
        appUser.setFirstname("Тест");
        appUser.setUsername("test_user");
    }

    @Test
    @DisplayName("Обработка первого сообщения если пользователя нет в системе")
    void executeFirstMessage() {
        SendMessage sendMessage = firstRegistrationCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("""
                Для начала давайте познакомимся с Вами.
                Для корректной работы мне нужно будет Вас запомнить.
                Если вы согласны на то чтобы я Вас запомнил напишите да.
                Если не хотите этого напишите нет.
                    """));
        assertNotNull(getRecordingUserInMap(123L));
        assertNotNull(getRecordingUserInMap(123L));
    }

    @Test
    @DisplayName("Обработка первого сообщения: пользователь уже есть в map но message пустой")
    void executeFirstMessageWithUserInMap() {
        setRecordingUserInMap(123L, "status_uncertain");
        SendMessage sendMessage = firstRegistrationCommand.executeFirstMessage(message);
        assertTrue(sendMessage.getText().contains("Не разобрал ваше сообщение"));
        assertNull(getRecordingUserInMap(123L));
    }

    @Test
    @DisplayName("Обработка второго сообщения: пользователь согласен на регистрацию")
    void executeNextMessageSuccessful() {
        Chat chat = new Chat();
        chat.setFirstName("Тест");
        chat.setUserName("test_user");
        when(message.getText())
                .thenReturn("да");
        when(message.getChat()).thenReturn(chat);
        setRecordingUserInMap(123L, "status_uncertain");
        Pair<SendMessage, String> pair = firstRegistrationCommand.executeNextMessage(message);
        assertNotNull(pair);
        assertTrue(pair.getFirst().getText().contains("Теперь можем продолжить работу.\nДля просмотра функций бота"));
        assertTrue(pair.getSecond().contains("non_sticker"));
        verify(appUserService).saveUser(appUser);
        assertNull(getRecordingUserInMap(123L));
    }
    @Test
    @DisplayName("Обработка второго сообщения: пользователь отказался от регистрацию")
    void executeNextMessageUserDoesntRegistration() {
        when(message.getText())
                .thenReturn("нет");
        setRecordingUserInMap(123L, "status_uncertain");
        Pair<SendMessage, String> pair = firstRegistrationCommand.executeNextMessage(message);
        assertNotNull(pair);
        assertTrue(pair.getFirst().getText().contains("Без вашего подтверждения на то чтобы я Вас запомнил,\n" +
                "к сожалению, я не смогу продожить работу с вами"));
        assertTrue(pair.getSecond().contains("non_sticker"));
        assertNull(getRecordingUserInMap(123L));
    }
    @Test
    @DisplayName("Обработка второго сообщения: отсутвие статуса пользователя в map")
    void executeNextMessageWithNotFoundTemporaryData() {
        when(message.getText())
                .thenReturn("да");
        assertThrows(NotFoundTemporaryDateUser.class, () ->firstRegistrationCommand.executeNextMessage(message));
    }

    private void setRecordingUserInMap(Long userId, String status) {
        try {
            Field mapRecordField = FirstRegistrationCommand.class.getDeclaredField("mapUser");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, String> mapRecord =
                    (ConcurrentHashMap<Long, String>) mapRecordField.get(firstRegistrationCommand);
            mapRecord.put(userId, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getRecordingUserInMap(Long chatId){
        try {
            Field mapRecordField = FirstRegistrationCommand.class.getDeclaredField("mapUser");
            mapRecordField.setAccessible(true);
            ConcurrentHashMap<Long, String> mapRecord =
                    (ConcurrentHashMap<Long, String>) mapRecordField.get(firstRegistrationCommand);
            return mapRecord.get(chatId);
        }catch (NoSuchFieldException | IllegalAccessException ex){
            throw new RuntimeException(ex);
        }
    }
}