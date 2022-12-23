package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final TypeController typeController;
    private final RecordingController recordingController;

    public List<BotApiMethod> checkCommand(CallbackQuery callbackQuery){
        String command = callbackQuery.getData();
        if (command.startsWith("edit")){
            List<BotApiMethod> methodList = typeController.checkCommandForTypeRecording(command, callbackQuery);
            return methodList;
        } else if (command.startsWith("recording")) {
          return recordingController.checkCommandForRecording(command, callbackQuery);
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Что то пошло не так, попробуйте еще раз...");

        return List.of(sendMessage);
    }

    public List<BotApiMethod> commandToTypeCreate(Update update){
        return typeController.createNewType(update);
    }
}
