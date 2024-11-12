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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTypeRecordCommandTest {
    @Mock
    private AppUserService appUserService;
    @Mock
    private TypeRecordingService typeRecordingService;
    @InjectMocks
    private DeleteTypeRecordCommand deleteTypeRecordCommand;

    @Mock
    private Message message;
    private AppUser appUser;
    @BeforeEach
    void setUp() {
        when(message.getChatId()).
                thenReturn(123L);
        appUser = new AppUser();
        appUser.setId(1L);
        appUser.setTelegramUserId(123L);
    }

    @Test
    @DisplayName("Успешное удаление типа услуги")
    void executeFirstMessageSuccessful() {
        TypeRecording typeRecording = createTypeRecording(1L, "маникюр", 3000.0);
        when(message.getText())
                .thenReturn("удали услугу маникюр");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(typeRecording.getTypeName(), appUser.getId()))
                .thenReturn(List.of(typeRecording));
        SendMessage sendMessage = deleteTypeRecordCommand.executeFirstMessage(message);

        verify(typeRecordingService).findByTypeNameIgnoreCaseAndAppUserId(typeRecording.getTypeName(),
                appUser.getId());
        verify(typeRecordingService).delete(typeRecording);
        assertTrue(sendMessage.getText().contains(String.format("Удалил услугу: %s, стоимостью: %s",
                typeRecording.getTypeName(),
                typeRecording.getTypeCoast())));

    }

    @Test
    @DisplayName("Обработка удаления услуги если пользователь отсутствует")
    void executeFirstMessageWithNoUser(){
        when(message.getText())
                .thenReturn("удали услугу маникюр");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> deleteTypeRecordCommand.executeFirstMessage(message));
    }

    @Test
    @DisplayName("Обработка удаления услуги если услуга отсутствует")
    void executeFirstMessageWithIncorrectDate(){
        when(message.getText())
                .thenReturn("удали услугу педикюр");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId("педикюр", appUser.getId()))
                .thenReturn(List.of());
        SendMessage sendMessage = deleteTypeRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText()
                .contains("Не смог удалить тип услуги: педикюр, потому что не нашел у вас такой услуги"));
    }
    @Test
    @DisplayName("Обработка удаления при некорректрной услуге")
    void executeFirstMessageWithIncorrectMessage(){
        when(message.getText())
                .thenReturn("удали услугу xcvxc");
        when(appUserService.findAppUserByTelegramId(123L))
                .thenReturn(Optional.of(appUser));
        when(typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId("xcvxc", appUser.getId()))
                .thenReturn(List.of());
        SendMessage sendMessage = deleteTypeRecordCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText()
                .contains("Не смог удалить тип услуги: xcvxc"));
    }

    private TypeRecording createTypeRecording(Long id, String name, Double coast){
        return TypeRecording.builder()
                .id(id)
                .typeName(name)
                .typeCoast(coast)
                .appUser(appUser)
                .build();
    }
}