package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telproject.service.interfac.AppUserService;
import ru.telproject.service.interfac.UpdateProducer;
import ru.telproject.utils.InlineButtonUtils;
import ru.telproject.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.telproject.model.RabbitQueue.*;

@Component
@Log4j
@RequiredArgsConstructor
public class UpdateController {
    private TelegramBot telegramBot;
    private final MessageUtils messageUtils;
    private final UpdateProducer updateProducer;
    private final RedirectController redirectController;
    private final AppUserService appUserService;

    public void registerBot(TelegramBot telegramBot){
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update){
        if (update == null) {
            log.error("Received update is null");
            return;
        }

        if (update.getCallbackQuery() != null){
            processTextMessageCallback(update);
            log.debug("Data message - " + update.getCallbackQuery().getData());
        }

        if (update.hasMessage()) {
            distributeMessageByType(update);
        }

    }

    public void distributeMessageByType(Update update){
        Message message = update.getMessage();
        if (message.hasText()){
            processTextMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        SendMessage sendMessage = messageUtils.generateSendMessageWithText(update,
                "Неподдерживаемый тип сообщения!");
        setView(sendMessage);
    }

    public void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    public void editMessage(BotApiMethod botApiMethod){
        telegramBot.sendEditMessage(botApiMethod);
    }

    private void processTextMessage(Update update) {

        String command = update.getMessage().getText();
        if (command.equals("menu")){
            sendMenu(update);
        }
        if (command.equals("help")){
            sendHelp(update);
        }
        if (command.equals("start")){
            checkUser(update);
        }
        if (command.toLowerCase().startsWith("создать услугу")){
            redirectController.commandToTypeCreate(update);
        }
    }
    private void processTextMessageCallback(Update update) {
        String command = update.getCallbackQuery().getData();
        SendMessage sendMessage = new SendMessage();
        List<BotApiMethod> methodList = redirectController.checkCommand(update.getCallbackQuery());

        methodList.forEach(this::editMessage);
    }

    public void sendMenu(Update update){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton typeButton = new InlineKeyboardButton();
        Map<String, String> menuMap = new HashMap<>();
        menuMap.put("Посмотреть/редактировать услуги", "edit_type");
        menuMap.put("Посмотреть/редактировать записи", "recording_menu");
        List<List<InlineKeyboardButton>> menuList = menuMap.entrySet().stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getKey(), o.getValue()))
                .map(InlineButtonUtils::getArrayTypeInline)
                .toList();
        markup.setKeyboard(menuList);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Что вас интересует %s?".formatted(update.getMessage().getFrom().getFirstName()));
        setView(sendMessage);
    }

    public void sendHelp(Update update){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Что умеет этот бот? \n Работать с записями и типами услуг");
        setView(sendMessage);
    }

    public void checkUser(Update update){
        if (appUserService.findByTelegramId(update.getMessage().getFrom().getId()).isPresent()){
            sendMenu(update);
        }else {
            appUserService.createUser(update);
        }
    }

}
