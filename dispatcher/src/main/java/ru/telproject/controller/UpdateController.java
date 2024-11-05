package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.exception.NotFoundStickerException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TextRecognizer;
import ru.telproject.service.command.CommandPull;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.CommandUtils;

import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j
@RequiredArgsConstructor
public class UpdateController {
    private TelegramBot telegramBot;
    private final AppUserService appUserService;
    private final TextRecognizer textRecognizer;
    private final CommandPull commandPull;
    private final ConcurrentHashMap<Long, String> userStates = new ConcurrentHashMap<>();

    public void registerBot(TelegramBot telegramBot){
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update){
        if (!checkUser(update)){
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            Long chatId = message.getChatId();
            String intent = textRecognizer.recognizeIntent(messageText.toLowerCase());
            if (userStates.containsKey(chatId)){
                Command command = commandPull.getCommand(userStates.get(chatId));
                Pair<SendMessage, String> sendMessageStringPair = command.executeNextMessage(message);
                SendMessage returnMessage = sendMessageStringPair.getFirst();
                returnMessage.setChatId(chatId);
                telegramBot.sendMessage(returnMessage);
                SendSticker sticker = sendSticker(sendMessageStringPair.getSecond());
                sticker.setChatId(chatId);
                telegramBot.setSticker(sticker);
                userStates.remove(chatId);
            }else {
                Command command = commandPull.getCommand(intent);
                if (command == null) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setText("Не совсем понял чего вы хотите, попробуйте еще раз или обратитесь к помощи бота\n" +
                            "Напишите:\n" +
                            "Помощь\n" +
                            "Info\n" +
                            "Что умеет бот?\n");
                    sendMessage.setChatId(chatId);
                    telegramBot.sendMessage(sendMessage);
                    SendSticker sticker = sendSticker("classpath:sticker/dont_understand.webm");
                    sticker.setChatId(chatId);
                    telegramBot.setSticker(sticker);
                }else {
                    SendMessage returnMessage = command.executeFirstMessage(message);
                    returnMessage.setChatId(chatId);
                    if (CommandUtils.hasExecuteNextMessageImplementation(command)) {
                        userStates.put(chatId, intent);
                    }
                    telegramBot.sendMessage(returnMessage);
                }
            }
        }
    }

    public SendSticker sendSticker(String path){
        SendSticker sticker = new SendSticker();
        try {
            sticker.setSticker(new InputFile(ResourceUtils.getFile(path)));
        } catch (FileNotFoundException e) {
            throw new NotFoundStickerException(e.getMessage() + "path: " + path);
        }
        return sticker;
    }

    public boolean checkUser(Update update){
        if (appUserService.findAppUserByTelegramId(update.getMessage().getChatId()).isPresent()){
            return true;
        }else {
            Command firstRegistrationCommand = commandPull.getCommand("first_registration_command");
            SendMessage sendMessage = firstRegistrationCommand.executeFirstMessage(update.getMessage());
            sendMessage.setChatId(update.getMessage().getChatId());
            sendMessage(sendMessage);
            return false;
        }
    }

    public void sendMessage(SendMessage sendMessage){
        telegramBot.sendMessage(sendMessage);
    }

}
