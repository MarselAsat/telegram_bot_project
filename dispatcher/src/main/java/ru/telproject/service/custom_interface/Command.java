package ru.telproject.service.custom_interface;

import org.springframework.data.util.Pair;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface Command {
    SendMessage executeFirstMessage(Message message);
    default Pair<SendMessage, String> executeNextMessage(Message message){return null;};
}
