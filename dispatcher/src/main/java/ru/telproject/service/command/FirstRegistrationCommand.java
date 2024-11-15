package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.exception.NotFoundTemporaryDateUser;
import ru.telproject.service.AppUserService;
import ru.telproject.service.MetricsService;
import ru.telproject.service.custom_interface.Command;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirstRegistrationCommand implements Command {
    private final AppUserService appUserService;
    private final ConcurrentHashMap<Long, String> mapUser = new ConcurrentHashMap<>();
    private final MetricsService metricsService;

    @Override
    public SendMessage executeFirstMessage(Message message) {
        return metricsService.recordingTime("first_registration_execute_time", () -> {
            log.info("Processing registration user first message for chat ID: {}", message.getChatId());
            Long telegramUserId = message.getChatId();
            SendMessage sendMessage = new SendMessage();
            if (!mapUser.containsKey(telegramUserId)) {
                sendMessage.setText("""
                        Для начала давайте познакомимся с Вами.
                        Для корректной работы мне нужно будет Вас запомнить.
                        Если вы согласны на то чтобы я Вас запомнил напишите да.
                        Если не хотите этого напишите нет.
                        """);
                mapUser.put(message.getChatId(), "status_uncertain");
            } else {
                Pair<SendMessage, String> returnMessage = executeNextMessage(message);
                sendMessage = returnMessage.getFirst();
            }
            log.info("Successfully first message registration: {}", message.getText());
            metricsService.incrementCounter("first_registration_execute_successful", "user_id",
                    telegramUserId.toString());
            return sendMessage;
        });
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        return metricsService.recordingTime("first_registration_execute_time", () -> {
        log.info("Processing registration user next message for chat ID: {}", message.getChatId());
        String messageText = message.getText();
        Long telegrmUserId = message.getChatId();
        if (!mapUser.containsKey(telegrmUserId)){
            metricsService.incrementCounter("first_registration_error_counter",
                    "error", "user_not_found",
                    "user_id", telegrmUserId.toString());
            throw new NotFoundTemporaryDateUser("то то пошло не так, попробуйте еще раз.");
        }
        SendMessage sendMessage = new SendMessage();
        if (!StringUtils.hasText(messageText)){
            sendMessage.setText("Не разобрал ваше сообщение. попробуйте пройти регистрацию еще раз");
            mapUser.remove(telegrmUserId);
            metricsService.incrementCounter("first_registration_error_counter",
                    "error", "dont_parse_message",
                    "user_id", telegrmUserId.toString());
            return Pair.of(sendMessage, "non_sticker");
        }
        if (messageText.trim().toLowerCase().contains("да")){
            AppUser user = AppUser.builder().telegramUserId(telegrmUserId)
                    .firstname(message.getChat().getFirstName())
                    .username(message.getChat().getUserName())
                    .build();
            appUserService.saveUser(user);
            sendMessage.setText("""
                Теперь можем продолжить работу.
                Для просмотра функций бота обратитесь к помощи бота
                Напишите: Что умеет бот?""");
            log.info("Successfully registration user chat ID: {}", message.getChatId());
            metricsService.incrementCounter("first_registration_execute_successful", "user_id",
                    telegrmUserId.toString());
        }else {
            sendMessage.setText("""
                    Без вашего подтверждения на то чтобы я Вас запомнил,
                    к сожалению, я не смогу продожить работу с вами.
                    Если все же передумаете я Вас всегда жду.
                    """);
            metricsService.incrementCounter("first_registration_execute",
                    "error", "user_dont_agree",
                    "user_id", telegrmUserId.toString());
        }
        mapUser.remove(telegrmUserId);
        return Pair.of(sendMessage, "non_sticker");
        });
    }
}
