package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.RecordingUser;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.MetricsService;
import ru.telproject.service.RecordingService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.TextFinderUtils;
import ru.telproject.validator.RecordingValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final RecordingService recordingService;
    private final MetricsService metricsService;
    private final ConcurrentHashMap<Long, RecordingUser> mapRecord = new ConcurrentHashMap<>();

    @Override
    public SendMessage executeFirstMessage(Message message) {
        return metricsService.recordingTime("create-record_first_message_time", () -> {
            log.info("Processing create record first message for chat ID: {}", message.getChatId());
            LocalDateTime dateTimeRecord = parseDateTime(message.getText());
            RecordingUser recordingUser = recordingService.createRecording(message, dateTimeRecord);
            mapRecord.put(message.getChatId(), recordingUser);
            SendMessage sendMessage = createFirstMessage();
            log.info("Successfully processed create record message: {}", message.getText());
            metricsService.incrementCounter("create-record_first_message_successful",
                    "user_id", message.getChatId().toString());
            return sendMessage;
        });
    }


    private static LocalDateTime parseDateTime(String messageText) {
        LocalDate dateRecord = TextFinderUtils.getDateFromString(messageText);
        LocalTime timeRecord = TextFinderUtils.getLocalTimeFromString(messageText);
        return dateRecord.atTime(timeRecord);
    }

    private static SendMessage createFirstMessage() {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Если вы желаете добавить описание к записи, " +
                "например, номер телефона или имя, то напишите в ответ на это сообщение эти данные." +
                "Если не хотите добавлять описание то просто напишите: нет");
        return sendMessage;
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
       return metricsService.recordingTime("create-record_execute_next_message_time", () -> {
           try {
            log.info("Processing create record next message chat ID: {}", message.getChatId());
            String messageText = message.getText();
            Long telegramUserId = message.getChatId();
            RecordingUser recordingUser = mapRecord.get(telegramUserId);
            if (recordingUser == null) {
                throw new NotFoundTemporaryDateUser("Что то пошло не так, попробуйте еще раз." +
                        "Не получилось найти ваши данные для создания записи");
            }
            Pair<SendMessage, String> pair = createNextMessage(recordingUser, messageText);
            mapRecord.remove(telegramUserId);
            recordingUserRepository.save(recordingUser);
            log.info("Successfully create record processed next message: {}. recordingID={}",
                    message.getText(),
                    recordingUser.getId());
            return pair;
           }catch (NotFoundTemporaryDateUser ex){
               metricsService.incrementCounter("create-record_next_message_error",
                       "error", ex.getClass().getSimpleName(),
                       "user_id", message.getChatId().toString());
               throw ex;
           }
        });
    }

    private Pair<SendMessage, String> createNextMessage(RecordingUser recordingUser, String messageText) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Создал запись на ").append(recordingUser.getTypeRecording().getTypeName())
                .append("\nДата записи: ")
                .append(recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm")));
        if (!messageText.trim().toLowerCase().contains("нет")) {
            recordingUser.setDescription(messageText);
            stringBuffer.append("\nОписание к записи: ").append(messageText);
        }
        SendMessage sendMessage = buildResponseMessage(recordingUser);
        sendMessage.setText(stringBuffer.toString());
        Pair<SendMessage, String> pair = Pair.of(sendMessage, "classpath:sticker/saveit.webm");

        return pair;
    }

    private SendMessage buildResponseMessage(RecordingUser recordingUser){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(recordingUser.getAppUser().getTelegramUserId());
        return sendMessage;
    }


}
