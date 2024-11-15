package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.StemmingUtils;
import ru.telproject.utils.TextFinderUtils;

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
public class CreateRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    private final ConcurrentHashMap<Long, RecordingUser> mapRecord = new ConcurrentHashMap<>();

    @SneakyThrows
    @Override
    public SendMessage executeFirstMessage(Message message) {
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        AppUser appUser = findAppUserByTelegramId(telegramUserId);
        List<TypeRecording> typeRecordings = findTypeRecordings(appUser, messageText);
        LocalDateTime dateTimeRecord = parseDateTime(messageText);
        RecordingUser recordingUser = RecordingUser.builder().typeRecording(typeRecordings.get(0))
                .appUser(appUser).recordingTime(dateTimeRecord).build();
        mapRecord.put(telegramUserId, recordingUser);
        SendMessage sendMessage = createFirstMessage();
        return sendMessage;
    }

    private AppUser findAppUserByTelegramId(Long telegramUserId) {
        return appUserService.findAppUserByTelegramId(telegramUserId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private List<TypeRecording> findTypeRecordings(AppUser appUser, String messageText) throws IOException {
        List<TypeRecording> allByAppUserId = typeRecordingService.findAllByAppUserId(appUser.getId());
        List<String> typeNames = allByAppUserId.stream()
                .map(typeRecording -> typeRecording.getTypeName()).collect(Collectors.toList());
        List<String> stemmedWords = StemmingUtils.stemWords(typeNames);
        String regexTypeNames = stemmedWords.stream().collect(Collectors.joining("[аеиуой]|"));
        String typeName = "";
        Matcher matcher = TextFinderUtils.findRecordOnText("(" + regexTypeNames + "[аеиуой])\\s+", messageText);
        while (matcher.find()) {
            typeName = matcher.group(1);
        }
        String filterValue = StemmingUtils.stemWords(List.of(typeName)).get(0);
        typeName = typeNames.stream().filter(t -> t.contains(filterValue)).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Услуга: %s не найдена", filterValue)));
        List<TypeRecording> typeRecordings = typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(typeName,
                appUser.getId());
        return typeRecordings;
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
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        RecordingUser recordingUser = mapRecord.get(telegramUserId);
        mapRecord.remove(telegramUserId);
        recordingUserRepository.save(recordingUser);
        Pair<SendMessage, String> pair = createNextMessage(recordingUser, messageText);
        return pair;
    }

    private static Pair<SendMessage, String> createNextMessage(RecordingUser recordingUser, String messageText) {
        SendMessage sendMessage = new SendMessage();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Создал запись на ").append(recordingUser.getTypeRecording().getTypeName())
                .append("\nДата записи: ")
                .append(recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm")));
        if (!messageText.trim().toLowerCase().contains("нет")) {
            recordingUser.setDescription(messageText);
            stringBuffer.append("\nОписание к записи: ").append(messageText);
        }
        sendMessage.setText(stringBuffer.toString());
        Pair<SendMessage, String> pair = Pair.of(sendMessage, "classpath:sticker/saveit.webm");
        return pair;
    }


}
