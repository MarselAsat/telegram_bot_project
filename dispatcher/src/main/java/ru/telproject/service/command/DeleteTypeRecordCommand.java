package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.MetricsService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteTypeRecordCommand implements Command {
    private final AppUserService appUserService;
    private final TypeRecordingService typeRecordingService;
    private final MetricsService metricsService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        return metricsService.recordingTime("delete-type_first_message_time", () -> {
            log.info("Processing delete type record first message for chat ID: {}", message.getChatId());
            String text = message.getText();
            Long telegramUserId = message.getChatId();
            String typeName = "";
            SendMessage sendMessage = new SendMessage();
            Pattern pattern = Pattern.compile("(услугу|услуги)\\s+(\\p{L}+)", Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                typeName = matcher.group(2);
            }
            AppUser appUser = appUserService.findAppUserByTelegramId(telegramUserId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
            List<TypeRecording> typeRecordings = typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(typeName, appUser.getId());
            if (typeRecordings.size() == 0) {
                sendMessage.setText(String.format("Не смог удалить тип услуги: %s, потому что не нашел у вас такой услуги", typeName));
                metricsService.incrementCounter("delete-type_first_message_dont_delete",
                        "error", "not_found_type",
                        "user_id", message.getChatId().toString());
                return sendMessage;
            } else {
                TypeRecording typeRecording = typeRecordings.get(0);
                typeRecordingService.delete(typeRecording);
                sendMessage.setText(String.format("Удалил услугу: %s, стоимостью: %s", typeRecording.getTypeName(),
                        typeRecordings.get(0).getTypeCoast()));
                log.info("Successfully processed delele type record message: {}, typeRecordId={}", message.getText(),
                        typeRecording.getId());
                metricsService.incrementCounter("delete-type_first_message_successful",
                        "user_id", message.getChatId().toString());
                return sendMessage;
            }
        });
    }
}
