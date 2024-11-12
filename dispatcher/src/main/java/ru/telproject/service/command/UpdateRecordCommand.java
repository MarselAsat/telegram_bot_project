package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.InvalidRecordTypeException;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.RecordingUserNotFoundExceprion;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.MetricsService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.TextFinderUtils;
import ru.telproject.validator.RecordingValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateRecordCommand implements Command {
    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    private final RecordingUserRepository recordingUserRepository;
    private final ConcurrentHashMap<Long, RecordingUser> mapRecord = new ConcurrentHashMap<>();
    private final RecordingValidator recordingValidator;
    private final MetricsService metricsService;

    @Override
    public SendMessage executeFirstMessage(Message message) {
         return metricsService.recordingTime("update_record_first_message_time", () -> {
            log.info("Processing update record first message for chat ID: {}", message.getText());
            String messageText = message.getText();
            Long telegramUserId = message.getChatId();
            SendMessage sendMessage = new SendMessage();
            AppUser appUser = findAppUserByTelegramId(telegramUserId);
            List<TypeRecording> allTypeByAppUserId = typeRecordingService
                    .findAllByAppUserId(appUser.getId());
            String regex = allTypeByAppUserId.stream().map(t -> t.getTypeName()).collect(Collectors.joining("|"));
            Matcher matcher = TextFinderUtils.findRecordOnText("(" + regex + ")(\\s+)?", messageText);
            List<String> typeRecordName = new ArrayList<>();
            while (matcher.find()) {
                typeRecordName.add(matcher.group());
            }
            String finalTypeRecordName = typeRecordName.get(0).trim();
            TypeRecording typeRecording = allTypeByAppUserId.stream()
                    .filter(t -> t.getTypeName().equalsIgnoreCase(finalTypeRecordName))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRecordTypeException("Тип записи не найден"));
            List<LocalDate> allDateFromString = TextFinderUtils.extractAllDates(messageText);
            List<LocalTime> allLocalTimeFromString = TextFinderUtils.getAllLocalTimeFromString(messageText);
            if (allDateFromString.size() == 1) {
                allDateFromString.addAll(allDateFromString);
            }
            if (allLocalTimeFromString.size() == 1) {
                allLocalTimeFromString.addAll(allLocalTimeFromString);
            }
            List<LocalDateTime> dateTimes = new ArrayList<>();
            for (int i = 0; i < allDateFromString.size(); i++) {
                dateTimes.add(allDateFromString.get(i).atTime(allLocalTimeFromString.get(i)));
            }
            recordingValidator.validatorRecordingTimeList(dateTimes);
            RecordingUser recordUser = recordingUserRepository
                    .findRecordsByTimeTypeIdUserID(dateTimes.get(0), typeRecording.getId(), appUser.getId())
                    .orElseThrow(() -> new RecordingUserNotFoundExceprion("Среди ваших записей не было найдено соответствующих вашему запросу"));
            if (typeRecordName.size() > 1) {
                String secondTypeName = typeRecordName.get(1);
                TypeRecording secondRecordType = allTypeByAppUserId.stream()
                        .filter(t -> t.getTypeName().equalsIgnoreCase(secondTypeName)).findFirst().get();
                recordUser.setTypeRecording(secondRecordType);
            } else {
                recordUser.setRecordingTime(dateTimes.get(1));
            }
            mapRecord.put(telegramUserId, recordUser);
            sendMessage.setText("Желаете изменить описание к записи?\n" +
                    "Текущее описание:" + recordUser.getDescription() +
                    "\nЕсли не желаете менять описание, напишите: нет");
            log.info("Successfully update record first message for chat ID: {}", message.getText());
            metricsService.incrementCounter("update_record_first_message_successful_counter",
                    "user_id", telegramUserId.toString());
            return sendMessage;
        });
    }

    private AppUser findAppUserByTelegramId(Long telegramUserId) {
        return appUserService.findAppUserByTelegramId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
         return metricsService.recordingTime("update_record_next_message_time", () -> {
            log.info("Processing update record next message for chat ID: {}", message.getText());
            String messageText = message.getText();
            Long telegramUserId = message.getChatId();
            SendMessage sendMessage = new SendMessage();
            RecordingUser recordingUser = mapRecord.get(telegramUserId);
            if (recordingUser == null) {
                metricsService.incrementCounter("update_record_next_message_error_counter",
                        "error", "record_not_found",
                        "user_id", telegramUserId.toString());
                throw new NotFoundTemporaryDateUser("Что то пошло не так, попробуйте еще раз");
            }
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("Изменил запись на ").append(recordingUser.getTypeRecording().getTypeName())
                    .append("\nДата записи: ")
                    .append(recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm")));
            if (!messageText.trim().toLowerCase().contains("нет")) {
                recordingUser.setDescription(messageText);
                stringBuffer.append("Описание к записи:").append(messageText);
            }
            mapRecord.remove(telegramUserId);
            recordingUserRepository.save(recordingUser);
            sendMessage.setText(stringBuffer.toString());
            Pair<SendMessage, String> pair = Pair.of(sendMessage, "classpath:sticker/saveit.webm");
            log.info("Successfully update record next message for chat ID: {}", message.getText());
            metricsService.incrementCounter("update_record_next_message_successful_counter",
                    "user_id", telegramUserId.toString());
            return pair;
        });
    }
}
