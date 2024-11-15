package ru.telproject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final TypeController typeController;
    private final RecordingController recordingController;

    public SendMessage checkCommand(Update update){
        String command = update.getCallbackQuery().getData();
        SendMessage sendMessage = new SendMessage();
        if (command.startsWith("edit")){
         return sendMessage = typeController.checkCommandForTypeRecording(command, update);
        } else if (command.startsWith("recording")) {
          return sendMessage = recordingController.checkCommandForRecording(command, update);
        }
        sendMessage.setText("Что то пошло не так, попробуйте еще раз...");

        return sendMessage;
    }

    public SendMessage commandToTypeCreate(Update update){
        return typeController.createNewType(update);
    }
}
