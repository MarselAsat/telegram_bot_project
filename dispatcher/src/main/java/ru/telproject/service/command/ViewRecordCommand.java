package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.TextFinderUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ViewRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final AppUserService appUserService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage  = new SendMessage();
        LocalDate dateFromString = TextFinderUtils.getDateFromString(messageText);
        if (dateFromString != null) {
            sendMessage = returnSendMessageOnDate(telegramUserId, dateFromString, sendMessage);
        }else {
            sendMessage.setText("На какую дату вы хотите посмотреть записи?");
        }
        return sendMessage;
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        LocalDate date = TextFinderUtils.getDateFromString(messageText);
        returnSendMessageOnDate(telegramUserId, date, sendMessage);
        return Pair.of(sendMessage, "null");
    }

    private SendMessage returnSendMessageOnDate(Long telegramUserId, LocalDate date, SendMessage sendMessage) {
        Optional<AppUser> byTelegramUserId = appUserService.findAppUserByTelegramId(telegramUserId);
        LocalDateTime atStartOfDay = date.atStartOfDay();
        LocalDateTime atEndOfDay = date.atTime(23, 59);
        List<RecordingUser> recordingUsers = recordingUserRepository.
                findByAppUserIdAndTimeBetween(byTelegramUserId.orElseThrow().getId(), atStartOfDay, atEndOfDay);
        String allRecordingsOnDate = recordingUsers.stream().map(record -> recordMapRoStringMessage(record))
                .collect(Collectors.joining("\n"));
        sendMessage.setText("Ваши записи на " + date.toString() + "\n" + allRecordingsOnDate);
        return sendMessage;
    }

    private String recordMapRoStringMessage(RecordingUser recordingUser){
        StringBuffer stringBuffer = new StringBuffer();
        String prefix = "Запись в - ";
        String postfix = ";услуга - ";
        String description = "; комментарии - ";
        stringBuffer.append(prefix)
                .append(recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .append(postfix).append(recordingUser.getTypeRecording().getTypeName());
        if (StringUtils.hasText(recordingUser.getDescription())){
            stringBuffer.append(description).append(recordingUser.getDescription());
        }
        return stringBuffer.toString();
    }
}
