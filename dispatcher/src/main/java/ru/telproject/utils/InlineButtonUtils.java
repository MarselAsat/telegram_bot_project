package ru.telproject.utils;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class InlineButtonUtils {
    public static List<InlineKeyboardButton> getArrayTypeInline(InlineKeyboardButton... button){
        List<InlineKeyboardButton> list = new ArrayList<>(Arrays.stream(button).toList());
        return list;
    }

    public static InlineKeyboardButton getInlineKeyboardButton(String buttonText, String callbackData) {
        InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText(buttonText);
        keyboardButton.setCallbackData(callbackData);
        return keyboardButton;
    }
}
