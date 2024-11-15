package ru.telproject.service.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.MetricsService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTypeRecordCommand implements Command {
    @Getter
    private final ConcurrentHashMap<Long, String> userData = new ConcurrentHashMap<>();

    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    private final MetricsService metricsService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        return metricsService.recordingTime("create-type.first.message.time", () -> {
            log.info("Processing create type record first message for chat ID: {}", message.getChatId());
            String text = message.getText();
            Pattern pattern = Pattern.compile("(услугу|услуги)\\s+(\\p{L}+)", Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(text);
            String typeName = "";
            if (matcher.find()) {
                typeName = matcher.group(2);
                userData.put(message.getChatId(), typeName);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setText(String.format("Какая цена будет у услуги %s?", typeName));
                log.info("Successfully processed create type record message: {}", message.getText());
                metricsService.incrementCounter("create-type.first.message.successful.counter",
                        "user_id", message.getChatId().toString());
                return sendMessage;
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setText("Не смог понять какую именно услугу выхотите создать.\nПопробуйте еще раз");
                metricsService.incrementCounter("create-type.first.message.error",
                        "error", "error_dont_matcher",
                        "user_id", message.getChatId().toString());
                return sendMessage;
            }
        });
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        return metricsService.recordingTime("create-type_next_message_time", () -> {
            log.info("Processing create type record next message chat ID: {}", message.getChatId());
            String messageText = message.getText();
            Pattern pattern = Pattern.compile("\\b(\\d+.\\d+|\\d+)");
            Matcher matcher = pattern.matcher(messageText);
            SendMessage sendMessage = new SendMessage();
            Long telegramUserID = message.getChatId();
            if (matcher.find()) {
                double price = Double.parseDouble(matcher.group());
                TypeRecording typeRecording = new TypeRecording();
                if (userData.get(telegramUserID) == null) {
                    throw new NotFoundTemporaryDateUser("Что-то пошло не так попробуйте еще раз");
                }
                String typeName = userData.get(telegramUserID);
                AppUser appUser = appUserService.findAppUserByTelegramId(telegramUserID)
                        .orElseThrow(() -> new UserNotFoundException("Пользователь не найден. telegramUSerID: " + telegramUserID));
                List<TypeRecording> byTypeNameIgnoreCaseAndAppUserId = typeRecordingService
                        .findByTypeNameIgnoreCaseAndAppUserId(typeName, appUser.getId());
                if (byTypeNameIgnoreCaseAndAppUserId.size() == 0) {
                    typeRecording.setTypeName(typeName);
                    typeRecording.setTypeCoast(price);
                    Optional<AppUser> byTelegramUserId = appUserService.findAppUserByTelegramId(telegramUserID);
                    typeRecording.setAppUser(byTelegramUserId.orElseThrow());
                    typeRecordingService.saveTypeRecord(typeRecording);
                    sendMessage.setText(String.format("Создал услугу: %s \n цена услуги: %s", typeName, price));
                    log.info("Create type record userId={}, typeRecordID={}", message.getChatId(), typeRecording.getId());
                    userData.remove(telegramUserID);
                    metricsService.incrementCounter("create-type_next_message_successful",
                            "user_id", message.getChatId().toString());
                    return Pair.of(sendMessage, "classpath:sticker/yes-sir.webm");
                } else {
                    sendMessage.setText(String.format("У вас уже есть такая услуга: %s \nНе получится сохранить",
                            byTypeNameIgnoreCaseAndAppUserId.get(0).getTypeName()));
                    userData.remove(telegramUserID);
                    metricsService.incrementCounter("create-type_next_message_not-create", "error", "type-exists",
                            "user_id", message.getChatId().toString());
                    return Pair.of(sendMessage, "classpath:sticker/no-no-no.webm");
                }
            } else {
                sendMessage.setText("Не смог понять цену которую вы указали, попробуйте еще раз указать цену");
                userData.remove(telegramUserID);
                metricsService.incrementCounter("create-type_next_message_error",
                        "error", "error_dont_mather",
                        "user_id", message.getChatId().toString());
                return Pair.of(sendMessage, "classpath:dont_understand.webm");
            }
        });
    }
}
