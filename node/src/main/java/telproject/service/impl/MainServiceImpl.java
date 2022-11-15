package telproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import telproject.dao.RawDataDao;
import telproject.entity.RawData;
import telproject.service.MainService;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final RawDataDao rawDataDao;
    private final ProducerServiceImpl producerService;

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        Message message = update.getMessage();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("Save from NODE");

        producerService.producerAnswer(sendMessage);

    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();

        rawDataDao.save(rawData);
    }
}
