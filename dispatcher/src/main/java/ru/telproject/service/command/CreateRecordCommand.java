package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.RecordingService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.StemmingUtils;
import ru.telproject.utils.TextFinderUtils;
import ru.telproject.validator.RecordingValidator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final RecordingService recordingService;
    private final RecordingValidator recordingValidator;
    private final ConcurrentHashMap<Long, RecordingUser> mapRecord = new ConcurrentHashMap<>();

    @Override
    public SendMessage executeFirstMessage(Message message) {
        log.info("Processing create record first message for chat ID: {}", message.getChatId());
        LocalDateTime dateTimeRecord = parseDateTime(message.getText());
        recordingValidator.validatorRecordingTime(dateTimeRecord);
        RecordingUser recordingUser = recordingService.createRecording(message, dateTimeRecord);
        recordingValidator.validateTypeRecording(recordingUser.getTypeRecording());
        mapRecord.put(message.getChatId(), recordingUser);
        SendMessage sendMessage = createFirstMessage();
        log.info("Successfully processed create record message: {}", message.getText());
        return sendMessage;
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
        log.info("Processing create record next message chat ID: {}", message.getChatId());
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        RecordingUser recordingUser = mapRecord.get(telegramUserId);
        Pair<SendMessage, String> pair = createNextMessage(recordingUser, messageText);
        mapRecord.remove(telegramUserId);
        recordingUserRepository.save(recordingUser);
        log.info("Successfully create record processed next message: {}. recordingID={}",
                message.getText(),
                recordingUser.getId());
        return pair;
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
        return SendMessage.builder()
                .chatId(recordingUser.getAppUser().getTelegramUserId())
                .build();
    }


}
