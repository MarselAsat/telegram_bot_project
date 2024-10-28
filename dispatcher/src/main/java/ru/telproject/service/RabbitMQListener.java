package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telproject.controller.UpdateController;
import ru.telproject.entity.RecordingUser;
import ru.telproject.model.RabbitQueue;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {
private final UpdateController updateController;
    @RabbitListener(queues = RabbitQueue.RECORD_USER_PUSH)
    public void receiveMessage(RecordingUser recordingUser){
        SendMessage sendMessage = new SendMessage();
        String text =recordingUser.getAppUser().getFirstname() + "\nВаша предстоящая запись:\n- " +
                recordingUser.getTypeRecording().getTypeName() + "\nВремя записи: " +
                recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm"));
        sendMessage.setText(text);
        sendMessage.setChatId(recordingUser.getAppUser().getTelegramUserId());
        updateController.sendMessage(sendMessage);
    }
}
