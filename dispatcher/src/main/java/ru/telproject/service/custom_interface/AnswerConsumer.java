package ru.telproject.service.custom_interface;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface AnswerConsumer {
    void consume(SendMessage sendMessage);
}
