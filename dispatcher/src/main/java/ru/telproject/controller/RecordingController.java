package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telproject.utils.InlineButtonUtils;

import java.time.LocalDate;
import java.time.temporal.*;
import java.util.*;

@Controller
@Log4j
@RequiredArgsConstructor
public class RecordingController {

    public SendMessage checkCommandForRecording(String command, Update update){
        SendMessage sendMessage = new SendMessage();
        if (command.equals("recording_menu")){
            return sendRecordingMenu();

        }
        switch (command){
            case "recording_create":
                break;
            case "recording_all_see":
                break;
            case "recording_edit":
                break;
            case "recording_remove":
                break;
        }



        return new SendMessage();
    }

    public SendMessage sendRecordingMenu(){
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setText("Что вас интересует?");
        return sendMessage;
    }

    private List<List<InlineKeyboardButton>> getCalendar(String month, String mode, String endpoint){
        LocalDate date = null;

        if (month.isEmpty()){
            date = LocalDate.now();
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
}
