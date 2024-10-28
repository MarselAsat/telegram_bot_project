package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.service.custom_interface.UpdateProducer;


@Service
@Log4j
@RequiredArgsConstructor
public class UpdateProducerImpl implements UpdateProducer {

    private final RabbitTemplate rabbitTemplate;
    @Override
    public void produce(String rabbitQueue, Update update) {
        rabbitTemplate.convertAndSend(rabbitQueue, update);
    }
}
