package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
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
import ru.telproject.utils.TextFinderUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UpdateRecordCommand implements Command {
    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    private final RecordingUserRepository recordingUserRepository;
    private final ConcurrentHashMap<Long, RecordingUser> mapRecord = new ConcurrentHashMap<>();

    @Override
    public SendMessage executeFirstMessage(Message message) {
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        AppUser appUser = findAppUserByTelegramId(telegramUserId);
        List<TypeRecording> allTypeByAppUserId = typeRecordingService
                .findAllByAppUserId(appUser.getId());
        String regex = allTypeByAppUserId.stream().map(t -> t.getTypeName()).collect(Collectors.joining("|"));
        Matcher matcher = TextFinderUtils.findRecordOnText("(" + regex + ")\\s+", messageText);
        List<String> typeRecordName = new ArrayList<>();
        while (matcher.find()){
            typeRecordName.add(matcher.group());
        }
        String finalTypeRecordName = typeRecordName.get(0);
        TypeRecording typeRecording = allTypeByAppUserId.stream()
                .filter(t -> t.getTypeName().equalsIgnoreCase(finalTypeRecordName)).findFirst().get();
        List<LocalDate> allDateFromString = TextFinderUtils.getAllDateFromString(messageText);
        List<LocalTime> allLocalTimeFromString = TextFinderUtils.getAllLocalTimeFromString(messageText);
        List<LocalDateTime> dateTimes = new ArrayList<>();
        for (int i = 0; i < allDateFromString.size(); i++) {
            dateTimes.add(allDateFromString.get(i).atTime(allLocalTimeFromString.get(i)));
        }
        Optional<RecordingUser> record = recordingUserRepository
                .findRecordsByTimeTypeIdUserID(dateTimes.get(0), typeRecording.getId(), appUser.getId());
        RecordingUser recordUser = record.get();
        if (typeRecordName.size() > 1) {
            String secondTypeName = typeRecordName.get(1);
            TypeRecording secondRecordType = allTypeByAppUserId.stream()
                    .filter(t -> t.getTypeName().equalsIgnoreCase(secondTypeName)).findFirst().get();
            recordUser.setTypeRecording(secondRecordType);
        }else {
            recordUser.setRecordingTime(dateTimes.get(1));
        }
        mapRecord.put(telegramUserId, recordUser);
        sendMessage.setText("Желаете изменит описание к записи?\n" +
                "Текущее описание:" + recordUser.getDescription() +
                "\nЕсли не желаете менять описание, напишите: нет");
        return sendMessage;
    }

    private AppUser findAppUserByTelegramId(Long telegramUserId) {
        return appUserService.findAppUserByTelegramId(telegramUserId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        RecordingUser recordingUser = mapRecord.get(telegramUserId);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Изменил запись на").append(recordingUser.getTypeRecording().getTypeName())
                .append("\nДата записи: ")
                .append(recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm")));
        if (!messageText.trim().toLowerCase().contains("нет")){
            recordingUser.setDescription(messageText);
            stringBuffer.append("Описание к записи:").append(messageText);
        }
        mapRecord.remove(telegramUserId);
        recordingUserRepository.save(recordingUser);
        sendMessage.setText(stringBuffer.toString());
        Pair<SendMessage, String> pair = Pair.of(sendMessage, "classpath:sticker/saveit.webm");
        return pair;
    }
}
