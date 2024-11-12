package ru.telproject.service.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoViewCommandTest {

    @Mock
    private Message message;
    @Spy
    private InfoViewCommand infoViewCommand;
    @Test
    void executeFirstMessage() {
        when(message.getText())
                .thenReturn("test");
        SendMessage sendMessage = infoViewCommand.executeFirstMessage(message);
        assertNotNull(sendMessage);
        assertTrue(sendMessage.getText().contains("""
                Я бот-помощник мастерам предосталяющим услуги.
                С моей помощью вы можете вести свои записи.
                Вот что я могу:"""));
    }
}