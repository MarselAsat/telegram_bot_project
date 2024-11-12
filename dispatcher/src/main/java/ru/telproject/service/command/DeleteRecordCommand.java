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
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.RecordingUserNotFoundExceprion;
import ru.telproject.exception.TypeRecordingNotFoundException;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.MetricsService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.TextFinderUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final AppUserService appUserService;
    private final TypeRecordingService typeRecordingService;
    private final MetricsService metricsService;
    private ConcurrentHashMap<Long,RecordingUser> mapRecord = new ConcurrentHashMap<>();
    @Override
    public SendMessage executeFirstMessage(Message message) {
        return metricsService.recordingTime("delete-record_first_message_time", () -> {
            log.info("Processing delete record first message chat ID: {}", message.getChatId());
            String text = message.getText();
            Pattern pattern = Pattern.compile("(услугу|услуги)\\s+(\\p{L}+)", Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(text);
            Long telegramUserId = message.getChatId();
            SendMessage sendMessage = new SendMessage();
            String typeName = "";
            while (matcher.find()) {
                typeName = matcher.group(2);
            }
            LocalDate date = TextFinderUtils.getDateFromString(text);
            LocalTime time = TextFinderUtils.getLocalTimeFromString(text);
            LocalDateTime recordTime = date.atTime(time);
            AppUser appUser = appUserService.findAppUserByTelegramId(telegramUserId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
            TypeRecording typeRecording = typeRecordingService
                    .findByTypeNameIgnoreCaseAndAppUserId(typeName, appUser.getId())
                    .stream().findFirst()
                    .orElseThrow(() -> new TypeRecordingNotFoundException("Не найден такой тип записи для пользователя"));
            RecordingUser record = recordingUserRepository
                    .findRecordsByTimeTypeIdUserID(recordTime, typeRecording.getId(), appUser.getId())
                    .orElseThrow(() -> new RecordingUserNotFoundExceprion("Запись пользователя не найдена"));
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-dd-MMM HH:mm");
            if (record != null) {
                sendMessage.setText("""
                        Желаете удалить запись на услугу %s?
                        Дата и время записи: %s
                        Если хотите удалить запись напишите: да
                        """.formatted(typeRecording.getTypeName(), recordTime.format(dateTimeFormatter)));
                mapRecord.put(telegramUserId, record);
                log.info("Successfully processed delete record first message. chatId:{} message: {}",
                        telegramUserId,
                        message.getText());
                metricsService.incrementCounter("delete-record_first_message_successful",
                        "user_id", message.getChatId().toString());
            } else {
                sendMessage.setText("Не нашел такую запись у вас");
                metricsService.incrementCounter("delete-record_first_message_error",
                        "error", "not_found_record",
                        "user_id", message.getChatId().toString());
            }
            return sendMessage;
        });
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        return metricsService.recordingTime("delete-record_next_message_time", () -> {
            log.info("Processing delete record next message chatId: {}", message.getChatId());
            Long chatId = message.getChatId();
            String messageText = message.getText();
            SendMessage sendMessage;
            RecordingUser recordingUser = mapRecord.get(chatId);
            if (recordingUser == null) {
                metricsService.incrementCounter("delete-record_next_message_error",
                        "error", "temporary_date_not_user",
                        "user_id", message.getChatId().toString());
                throw new NotFoundTemporaryDateUser("Что то пошло не так, попробуйте еще раз. " +
                        "Не получилось найти ваши данные для удаления");
            }
            Pair<SendMessage, String> pair;
            if (messageText.toLowerCase().contains("да")) {
                recordingUserRepository.delete(recordingUser);
                sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(String.format("Запись на %s %s. Успешно удалена",
                        recordingUser.getTypeRecording().getTypeName(),
                        recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("YYYY-dd-MM HH:mm"))));
                mapRecord.remove(chatId);
                pair = Pair.of(sendMessage, "classpath:sticker/monkey.webm");
                log.info("Processing delete record next message. Successful delete chatId: {}, recordId: {}",
                        message.getChatId(),
                        recordingUser.getId());
                metricsService.incrementCounter("delete-record_next_message_successful",
                        "user_id", message.getChatId().toString());
            } else {
                sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(String.format("Отменил операцию удаления записи на %",
                        recordingUser.getTypeRecording().getTypeName()));
                mapRecord.remove(chatId);
                pair = Pair.of(sendMessage, "classpath:sticker/bisness-cat.webm.webm");
                log.info("Processing delete record next message. User refused to delete chatId: {}", message.getChatId());
                metricsService.incrementCounter("delete-record_next_message_dont_delete",
                        "user_id", message.getChatId().toString());
            }
            return pair;
        });
    }
}
