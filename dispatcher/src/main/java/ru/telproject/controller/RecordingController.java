package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.service.interfac.RecordingUserService;
import ru.telproject.service.interfac.TypeRecordingService;
import ru.telproject.utils.InlineButtonUtils;

import java.time.LocalDate;
import java.time.temporal.*;
import java.util.*;

@Controller
@Log4j
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingUserService recordingUserService;
    private final TypeRecordingService typeRecordingService;

    private final List<String> hours = List.of("01:", "03:", "04:", "05:", "06:", "07:", "08:", "09:", "10:", "11:", "12:",
            "13:", "14:", "15:", "16:", "17:", "18:", "19:", "20:", "21:", "22:", "23:", "24:");
    private final List<String> minutes = List.of("10", "20", "30", "40", "50", "60");

    public List<BotApiMethod> checkCommandForRecording(String command, CallbackQuery callbackQuery){
        SendMessage sendMessage = new SendMessage();
        if (command.equals("recording_menu")){
            return sendRecordingMenu(callbackQuery);

        }
        switch (command){
            case "recording_create_form":
                getCalendar(command);
                break;
            case "recording_all_see":
                break;
            case "recording_edit":
                break;
            case "recording_remove":
                break;
        }



        return null;
    }

    public List<BotApiMethod> sendRecordingMenu(CallbackQuery callbackQuery){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        Map<String, String> menuMap = new HashMap<>();
        menuMap.put("Создать запись", "recording_create_form");
        menuMap.put("Посмотреть существующие записи", "recording_see_all");
        menuMap.put("Редактировать запись", "recording_update");
        menuMap.put("Удалить запись", "recording_delete");
        List<List<InlineKeyboardButton>> menuList = menuMap.entrySet().stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getKey(), o.getValue()))
                .map(InlineButtonUtils::getArrayTypeInline)
                .toList();
        markup.setKeyboard(menuList);
        EditMessageReplyMarkup editMessageMarkup = new EditMessageReplyMarkup();
        editMessageMarkup.setReplyMarkup(markup);
        setIdForMarkup(callbackQuery, editMessageMarkup);
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText("Что вас интересует?");
        setIdForText(callbackQuery, editMessageText);
        return List.of(editMessageText, editMessageMarkup);
    }

    private List<List<InlineKeyboardButton>> getCalendar(String command){
        LocalDate date = null;
        String month = "";
        String endpoint = "";

        if (command.equals("recording_create_form")){
            date = LocalDate.now();
        }else {
            month = command.split(" ")[1];
            endpoint  = command.split(" ")[2];
        }

        if (month.equals("PREV") && StringUtils.hasText(month)){
            date = LocalDate.parse(month).minus(1, ChronoUnit.MONTHS);
            endpoint.replaceAll("PREV", "");
        }
        if (month.equals("SECOND") && StringUtils.hasText(month)){
            date = LocalDate.parse(month).plus(1, ChronoUnit.MONTHS);
            endpoint.replaceAll("SECOND", "");
        }
        List<List<InlineKeyboardButton>> calendar = new ArrayList<>();
        InlineKeyboardButton header = InlineButtonUtils.getInlineKeyboardButton(date.getMonth().name() + " " + date.getYear(), endpoint + "date: ");
        List<InlineKeyboardButton> headerLine = InlineButtonUtils.getArrayTypeInline(header);
        calendar.add(headerLine);
        for (int i = 0; i < date.getDayOfMonth(); i = i + 7) {
            List<InlineKeyboardButton> week = new ArrayList<>();
            for (int j = 1; j <= (i + 7) && j <= date.getDayOfMonth(); j++) {
                week.add(InlineButtonUtils.getInlineKeyboardButton("" + (i + j),
                        endpoint + "date: " + date.plus(i, ChronoUnit.DAYS) + "time - "));
            }
           calendar.add(week);
        }
        calendar.add(InlineButtonUtils.getArrayTypeInline(InlineButtonUtils.getInlineKeyboardButton("<", endpoint + "PREV")));
        calendar.add(InlineButtonUtils.getArrayTypeInline(InlineButtonUtils.getInlineKeyboardButton(">", endpoint + "SECOND")));
    return calendar;
    }

    private List<BotApiMethod> sendTimeRecording(CallbackQuery callbackQuery){
        //TODO допилить сервис
        if (callbackQuery.getData().contains("date:")){
            String data = callbackQuery.getData();
            String date = data.substring(data.lastIndexOf("date: "));
            List<RecordingUser> userRecordingsByDate = recordingUserService.findUserRecordingsByDate(callbackQuery.getFrom().getId(), date);
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            EditMessageText editText = new EditMessageText();
            StringBuffer stringBuffer = new StringBuffer();
            if (userRecordingsByDate.size() > 0) {
                stringBuffer.append("На данную дату уже есть записи: \n");
                userRecordingsByDate.forEach(o -> stringBuffer.append(o.getTypeRecording().getTypeName() + " - " + o.getRecordingTime()));
                stringBuffer.append("Выберите время для новых записей: ");
            }else {
                stringBuffer.append("Выберите время для записи: \n");
            }
            editText.setText(stringBuffer.toString());
            setIdForText(callbackQuery, editText);

            return null;
        }

        return null;
    }

    private List<List<InlineKeyboardButton>> sendHourMarkup(String query){
        List<List<InlineKeyboardButton>> hourButton = hours.stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o, query + " -hour: " + o))
                .map(InlineButtonUtils::getArrayTypeInline).toList();
        return hourButton;
    }

    private List<List<InlineKeyboardButton>> sendMinuteMarkup(String query){
        List<List<InlineKeyboardButton>> minutesButton = minutes.stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o, query + " -minutes: " + o))
                .map(InlineButtonUtils::getArrayTypeInline).toList();
        return minutesButton;
    }

    private List<List<InlineKeyboardButton>> sendTypes(String query, CallbackQuery callbackQuery){
        Long appUserTelegramId = callbackQuery.getFrom().getId();
        List<TypeRecording> allTypeByUser = typeRecordingService.findAllType(appUserTelegramId);
        List<List<InlineKeyboardButton>> typeList = allTypeByUser.stream()
                .map(o -> InlineButtonUtils.getInlineKeyboardButton(o.getTypeName(), query + " -type: " + o.getId()))
                .map(InlineButtonUtils::getArrayTypeInline).toList();
        return typeList;
    }

    private void setIdForMarkup(CallbackQuery callbackQuery, EditMessageReplyMarkup editMessageReplyMarkup) {
        editMessageReplyMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageReplyMarkup.setChatId(callbackQuery.getMessage().getChatId());
    }
    private void setIdForText(CallbackQuery callbackQuery, EditMessageText editMessage) {
        editMessage.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessage.setChatId(callbackQuery.getMessage().getChatId());
    }
}
