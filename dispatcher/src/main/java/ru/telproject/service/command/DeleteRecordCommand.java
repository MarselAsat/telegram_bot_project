package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DeleteRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final AppUserService appUserService;
    private final TypeRecordingService typeRecordingService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
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
        Optional<AppUser> byTelegramUserId = appUserService.findAppUserByTelegramId(telegramUserId);
        TypeRecording typeRecording = typeRecordingService
                .findByTypeNameIgnoreCaseAndAppUserId(typeName, byTelegramUserId.orElseThrow().getId())
                .get(0);
        Optional<RecordingUser> record = recordingUserRepository
                .findRecordsByTimeTypeIdUserID(recordTime, typeRecording.getId(), byTelegramUserId.orElseThrow().getId());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-dd-MMM HH:mm");
        if (record.isPresent()){
            sendMessage.setText("""
                    Желаете удалить запись на услугу %s?
                    Дата и время записи: %s
                    """.formatted(typeRecording.getTypeName(), recordTime.format(dateTimeFormatter)));
        }else {
            sendMessage.setText("Не нашел такую запись у вас");
        }
        return sendMessage;
    }
}
