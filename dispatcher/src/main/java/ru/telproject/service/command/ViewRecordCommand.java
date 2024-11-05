package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.RecordingUser;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.repository.RecordingUserRepository;
import ru.telproject.service.AppUserService;
import ru.telproject.service.custom_interface.Command;
import ru.telproject.utils.TextFinderUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewRecordCommand implements Command {
    private final RecordingUserRepository recordingUserRepository;
    private final AppUserService appUserService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        log.info("Processing view record first message for chat ID: {}", message.getText());
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage  = new SendMessage();
        LocalDate dateFromString;
        try {
            dateFromString = TextFinderUtils.getDateFromString(messageText);
        }catch (DateTimeParseException ex){
           sendMessage.setText("На какую дату вы хотите посмотреть записи?");
           return sendMessage;
        }
        String recordUserOnString = returnSendMessageOnDate(telegramUserId, dateFromString);
        if (StringUtils.hasText(recordUserOnString)){
            sendMessage.setText("Ваши записи на " + dateFromString + "\n" + recordUserOnString);
        }else {
            sendMessage.setText("У вас нет записей на " + dateFromString);
        }
        log.info("Successfully view record first message for chat ID: {}", message.getText());
        return sendMessage;
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        log.info("Processing view record next message for chat ID: {}", message.getText());
        String messageText = message.getText();
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        LocalDate date = TextFinderUtils.getDateFromString(messageText);
        String recordsOnString = returnSendMessageOnDate(telegramUserId, date);
        if (StringUtils.hasText(recordsOnString)){
            sendMessage.setText(recordsOnString);
        }else{
            sendMessage.setText("У вас нет записей на " + date);
        }
        log.info("Successfully view record next message for chat ID: {}", message.getText());
        return Pair.of(sendMessage, "null");
    }

    private String returnSendMessageOnDate(Long telegramUserId, LocalDate date) {
        AppUser byTelegramUserId = appUserService.findAppUserByTelegramId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        LocalDateTime atStartOfDay = date.atStartOfDay();
        LocalDateTime atEndOfDay = date.atTime(23, 59);
        List<RecordingUser> recordingUsers = recordingUserRepository.
                findByAppUserIdAndTimeBetween(byTelegramUserId.getId(), atStartOfDay, atEndOfDay);
        String allRecordingsOnDateString = recordingUsers.stream().map(record -> recordMapRoStringMessage(record))
                .collect(Collectors.joining("\n"));
        return allRecordingsOnDateString;
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
