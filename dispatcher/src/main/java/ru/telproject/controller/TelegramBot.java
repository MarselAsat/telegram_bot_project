package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final UpdateController updateController;
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }

    @PostConstruct
    public void init(){
        updateController.registerBot(this);
    }

    public void sendAnswerMessage(SendMessage sendMessage) {
        if (sendMessage != null) {
            try {
                execute(sendMessage);
                EditMessageReplyMarkup markup = new EditMessageReplyMarkup();
                markup.setReplyMarkup((InlineKeyboardMarkup) sendMessage.getReplyMarkup());
            }catch (TelegramApiException ex){
                log.error(ex);
            }
        }
    }

    @SneakyThrows
    public void setSticker(SendSticker sticker){
        execute(sticker);
    }

    @SneakyThrows
    public void sendMessage(SendMessage sendMessage){
        if (sendMessage != null) {
            execute(sendMessage);
        }
    }

    public void sendEditMessage(BotApiMethod botApiMethod){
        if (botApiMethod != null) {
            try {
                execute(botApiMethod);
            }catch (TelegramApiException ex){
                log.error(ex);
            }
        }
    }
}
