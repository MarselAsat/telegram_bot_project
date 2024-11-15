package telproject.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.model.RabbitQueue;
import telproject.service.ConsumeService;
import telproject.service.MainService;
import telproject.service.ProducerService;

import static ru.telproject.model.RabbitQueue.*;

@Service
@Log4j
@RequiredArgsConstructor
public class ConsumeServiceImpl implements ConsumeService {

    private final MainService mainService;

    @Override
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    public void consumeTextMessageUpdates(Update update) {
    log.debug("Node: Text message is received");
    mainService.processTextMessage(update);
    }

    @Override
    @RabbitListener(queues = DOC_MESSAGE_UPDATE)
    public void consumeDocMessageUpdates(Update update) {
        log.debug("NODE: Text message is received");
    }

    @Override
    @RabbitListener(queues = PHOTO_MESSAGE_UPDATE)
    public void consumePhotoMessageUpdates(Update update) {
        log.debug("NODE: Text message is received");
    }
}
