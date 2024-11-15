package ru.telproject.service.custom_interface;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateProducer {
    void produce(String rabbitQueue, Update update);
}
