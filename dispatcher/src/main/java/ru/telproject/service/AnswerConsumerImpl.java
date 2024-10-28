package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telproject.controller.UpdateController;
import ru.telproject.service.custom_interface.AnswerConsumer;

import static ru.telproject.model.RabbitQueue.ANSWER_MESSAGE_UPDATE;

@Service
@RequiredArgsConstructor
public class AnswerConsumerImpl implements AnswerConsumer {

    private final UpdateController updateController;

    @Override
    @RabbitListener(queues = ANSWER_MESSAGE_UPDATE)
    public void consume(SendMessage sendMessage) {
//        updateController.setView(sendMessage);
    }


}
