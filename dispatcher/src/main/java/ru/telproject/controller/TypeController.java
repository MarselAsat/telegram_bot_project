package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

    public SendMessage checkCommandForTypeRecording(String command, Update update){

        if (command.equals("edit_type")){
            return sendTypeMenu();
        }
        SendMessage sendMessage = new SendMessage();
        switch (command){
            case "edit_type_create_form":
                sendMessage = formForType("Для создания типа услуги напишите в формате");
                break;
            case "edit_type_see_all":
                sendMessage = formForSeeAllType(update);
                break;
            case "edit_type_delete":
                sendMessage = formForTypeDelete(update.getCallbackQuery().getFrom());
                break;
            case "edit_type_update":
                break;
        }

        if(command.startsWith("edit_type_delete_id")){
            Long typeId = Long.valueOf(command.substring(19));
            sendMessage = deleteType(typeId, update);
        }

        return sendMessage;
    }


    public SendMessage sendTypeMenu(){
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
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setText("Что вас интересует?");
        return sendMessage;
    }

    private SendMessage formForType(String startText){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("%s:\n".formatted(startText) +
                "создать услугу: \n" +
                "ваша_услуга-стоимость\n" +
                "Например:\n\n" +
                "создать услугу: \n" +
                "Стрижка-500");
        return sendMessage;
    }

    private SendMessage formForTypeDelete(User user){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Выберите услугу которую хотите удалить:\n");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<TypeRecording> allType = typeRecordingService.findAllType(user.getId());
        List<List<InlineKeyboardButton>> collectButton = allType.stream().
                map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getTypeName() + " - " + o.getTypeCoast(),
                        "edit_type_delete_id" + o.getId()))
                .map(InlineButtonUtils::getArrayTypeInline).toList();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>(collectButton);
        markup.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markup);
        return sendMessage;
    }

    private SendMessage formForSeeAllType(Update update){
        SendMessage sendMessage = new SendMessage();
        List<TypeRecording> allType = typeRecordingService.findAllType(update.getCallbackQuery().getFrom().getId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Ваш перечень услуг:\n");
        for (int i = 0; i < allType.size(); i++) {
            TypeRecording type = allType.get(i);
            stringBuffer.append("%s. Услуга: %s , стоимость услуги: %s \n".formatted(i+1, type.getTypeName(), type.getTypeCoast()));
        }
        sendMessage.setText(stringBuffer.toString());
        return sendMessage;
    }

    public SendMessage deleteType(Long typeId, Update update){
        SendMessage sendMessage = new SendMessage();
        typeRecordingService.deleteType(typeId);
        List<TypeRecording> allTypeAfterDelete = typeRecordingService.findAllType(update.getCallbackQuery().getFrom().getId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Ваш перечень услуг после удаления:\n");
        for (int i = 0; i < allTypeAfterDelete.size(); i++) {
            TypeRecording type = allTypeAfterDelete.get(i);
            stringBuffer.append("%s. Услуга: %s , стоимость услуги: %s \n".formatted(i+1, type.getTypeName(), type.getTypeCoast()));
        }
        sendMessage.setText(stringBuffer.toString());
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        return sendMessage;
    }

    public SendMessage createNewType(Update update){
        SendMessage sendMessage = new SendMessage();
        try {
            String command = update.getMessage().getText();
            String[] split = command.substring(command.indexOf(":")).split("-");
            Long appUserId = update.getMessage().getFrom().getId();
            List<TypeRecording> maybeType = typeRecordingService.findByTypeNameIgnoreCase(split[0].trim() ,appUserId);
            if (maybeType.size() > 0){
                sendMessage.setText("Данная услуга уже существует попробуйте другое имя для услуги");
                return sendMessage;
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
        return sendMessage;
    }
}
