package telproject.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumeService {
    void consumeTextMessageUpdates(Update update);
    void consumeDocMessageUpdates(Update update);
    void consumePhotoMessageUpdates(Update update);
}
