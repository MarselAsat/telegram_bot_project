package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telproject.entity.TypeRecording;
import ru.telproject.service.interfac.TypeRecordingService;
import ru.telproject.utils.InlineButtonUtils;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Log4j
public class TypeController {
    private final TypeRecordingService typeRecordingService;

    public List<BotApiMethod> checkCommandForTypeRecording(String command, CallbackQuery callbackQuery){

        if (command.equals("edit_type")){
            return sendTypeMenu(callbackQuery);
        }
        List<BotApiMethod> botApiMethod = new ArrayList<>();
        switch (command){
            case "edit_type_create_form":
                botApiMethod = formForType("Для создания типа услуги напишите в формате", callbackQuery);
                break;
            case "edit_type_see_all":
                botApiMethod = formForSeeAllType(callbackQuery);
                break;
            case "edit_type_delete":
                botApiMethod = formForTypeDelete(callbackQuery.getFrom(), callbackQuery);
                break;
            case "edit_type_update":
                break;
        }

        if(command.startsWith("edit_type_delete_id")){
            Long typeId = Long.valueOf(command.substring(19));
            botApiMethod = deleteType(typeId, callbackQuery);
        }

        return botApiMethod;
    }


    public List<BotApiMethod> sendTypeMenu(CallbackQuery callbackQuery){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        Map<String, String> menuMap = new HashMap<>();
        menuMap.put("Создать тип услуги", "edit_type_create_form");
        menuMap.put("Посмотреть существующие услуги", "edit_type_see_all");
        menuMap.put("Редактировать услугу", "edit_type_update");
        menuMap.put("Удалить слугу", "edit_type_delete");
        List<List<InlineKeyboardButton>> menuList = menuMap.entrySet().stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getKey(), o.getValue()))
                .map(InlineButtonUtils::getArrayTypeInline)
                .toList();
        markup.setKeyboard(menuList);
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setReplyMarkup(markup);
        setIdForMarkup(callbackQuery, editMessageReplyMarkup);
        List<BotApiMethod> methodList = new ArrayList<>();
        methodList.add(editMessageReplyMarkup);
        return methodList;
    }

    private List<BotApiMethod> formForType(String startText, CallbackQuery callbackQuery){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("%s:\n".formatted(startText) +
                "создать услугу: \n" +
                "ваша_услуга-стоимость\n" +
                "Например:\n\n" +
                "создать услугу: \n" +
                "Стрижка-500");
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        List<BotApiMethod> methodList = new ArrayList<>();
        methodList.add(sendMessage);
        return methodList;
    }

    private List<BotApiMethod> formForTypeDelete(User user, CallbackQuery callbackQuery){
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText("Выберите услугу которую хотите удалить:\n");
        setIdForText(callbackQuery, editMessageText);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<TypeRecording> allType = typeRecordingService.findAllType(user.getId());
        List<List<InlineKeyboardButton>> collectButton = allType.stream().
                map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getTypeName() + " - " + o.getTypeCoast(),
                        "edit_type_delete_id" + o.getId()))
                .map(InlineButtonUtils::getArrayTypeInline).toList();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>(collectButton);
        markup.setKeyboard(rowsInLine);
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setReplyMarkup(markup);
        setIdForMarkup(callbackQuery, editMessageReplyMarkup);

        List<BotApiMethod> methodList = new ArrayList<>();
        methodList.add(editMessageReplyMarkup);
        methodList.add(editMessageText);
        return methodList;
    }

    private void setIdForMarkup(CallbackQuery callbackQuery, EditMessageReplyMarkup editMessageReplyMarkup) {
        editMessageReplyMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageReplyMarkup.setChatId(callbackQuery.getMessage().getChatId());
    }
    private void setIdForText(CallbackQuery callbackQuery, EditMessageText editMessageText) {
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
    }

    private List<BotApiMethod> formForSeeAllType(CallbackQuery callbackQuery){
        SendMessage sendMessage = new SendMessage();
        List<TypeRecording> allType = typeRecordingService.findAllType(callbackQuery.getFrom().getId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Ваш перечень услуг:\n");
        for (int i = 0; i < allType.size(); i++) {
            TypeRecording type = allType.get(i);
            stringBuffer.append("%s. Услуга: %s , стоимость услуги: %s \n".formatted(i+1, type.getTypeName(), type.getTypeCoast()));
        }
        sendMessage.setText(stringBuffer.toString());
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        List<BotApiMethod> methodList = new ArrayList<>();
        methodList.add(sendMessage);
        return methodList;
    }

    public List<BotApiMethod> deleteType(Long typeId, CallbackQuery callbackQuery){
        SendMessage sendMessage = new SendMessage();
        typeRecordingService.deleteType(typeId);
        List<TypeRecording> allTypeAfterDelete = typeRecordingService.findAllType(callbackQuery.getFrom().getId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Ваш перечень услуг после удаления:\n");
        for (int i = 0; i < allTypeAfterDelete.size(); i++) {
            TypeRecording type = allTypeAfterDelete.get(i);
            stringBuffer.append("%s. Услуга: %s , стоимость услуги: %s \n".formatted(i+1, type.getTypeName(), type.getTypeCoast()));
        }
        sendMessage.setText(stringBuffer.toString());
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        return List.of(sendMessage);
    }

    public List<BotApiMethod> createNewType(Update update){
        SendMessage sendMessage = new SendMessage();
        List<BotApiMethod> methodList = new ArrayList<>();
        try {
            String command = update.getMessage().getText();
            String[] split = command.substring(command.indexOf(":")).split("-");
            Long appUserId = update.getMessage().getFrom().getId();
            List<TypeRecording> maybeType = typeRecordingService.findByTypeNameIgnoreCase(split[0].trim() ,appUserId);
            if (maybeType.size() > 0){
                sendMessage.setText("Данная услуга уже существует попробуйте другое имя для услуги");
                methodList.add(sendMessage);
                return methodList;
            }else {
                TypeRecording newType = typeRecordingService.createType(split[0].trim(), Double.valueOf(split[1]), appUserId);
                sendMessage.setText("Тип услуги создан:\n" +
                        "Услуга - " + newType.getTypeName() + "\n" +
                        "Стоимость - " + newType.getTypeCoast());

            }
        }catch (Exception ex){
            log.error(ex);
        }
        sendMessage.setChatId(update.getMessage().getChatId());

        log.info(sendMessage.getText().replace("\n", " "));
        methodList.add(sendMessage);
        return methodList;
    }
}
