package ru.telproject.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewTypeRecordCommandTest {
    @Mock
    private TypeRecordingService typeRecordingService;
    @Mock
    private AppUserService appUserService;
    @Mock
    private Message message;

    @InjectMocks
    private ViewTypeRecordCommand viewTypeRecordCommand;
    private AppUser appUser;
    private TypeRecording firstTypeRecording;
    private TypeRecording secondTypeRecording;

    @BeforeEach
    void setUp() {
        when(message.getChatId())
                .thenReturn(123L);

        appUser = new AppUser();
        appUser.setTelegramUserId(123L);
        appUser.setId(1L);

        firstTypeRecording = new TypeRecording();
        firstTypeRecording.setTypeName("маникюр");
        firstTypeRecording.setId(1L);
        firstTypeRecording.setTypeCoast(3000.0);
        firstTypeRecording.setAppUser(appUser);

        secondTypeRecording = new TypeRecording();
        secondTypeRecording.setTypeName("макияж");
        secondTypeRecording.setId(2L);
        secondTypeRecording.setTypeCoast(3500.0);
        secondTypeRecording.setAppUser(appUser);

    }

    @Test
    @DisplayName("Успешное отображение услуг пользователя")
    void executeFirstMessageSuccessful() {
        when(message.getText())
                .thenReturn("Покажи все мои услуги");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of(firstTypeRecording, secondTypeRecording));

        SendMessage sendMessage = viewTypeRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText().contains("Перечень ваших услуг:"));
        assertTrue(sendMessage.getText().contains("Имя услуги: маникюр, цена услуги: 3000.0"));
        assertTrue(sendMessage.getText().contains("Имя услуги: макияж, цена услуги: 3500.0"));
    }

    @Test
    void executeFirstMessageWithRecordTypeEmpty(){
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findAllByAppUserId(appUser.getId()))
                .thenReturn(List.of());
        SendMessage sendMessage = viewTypeRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText().contains("У вас пока нет созданных услуг"));
    }

    @Test
    @DisplayName("Ошибка при попытке просмотра пустым пользователем")
    void executeFirstMessageWithNoUser(){
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> viewTypeRecordCommand.executeFirstMessage(message));
    }


}