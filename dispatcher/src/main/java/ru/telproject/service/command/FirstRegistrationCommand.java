package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.service.AppUserService;
import ru.telproject.service.custom_interface.Command;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FirstRegistrationCommand implements Command {
    private final AppUserService appUserService;
    private final ConcurrentHashMap<Long, String> mapUser = new ConcurrentHashMap<>();
    @Override
    public SendMessage executeFirstMessage(Message message) {
        Long telegramUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        if (!mapUser.containsKey(telegramUserId)) {
            sendMessage.setText("""
                    Для начала давайте познакомимся с Вами.
                    Для корректной работы мне нужно будет Вас запомнить.
                    Если вы согласны на то чтобы я Вас запомнил напишите да.
                    Если не хотите этого напишите нет.
                    """);
            mapUser.put(message.getChat().getId(), "status_uncertain");
        }else {
            Pair<SendMessage, String> returnMessage = executeNextMessage(message);
            sendMessage = returnMessage.getFirst();
        }
        return sendMessage;
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        String messageText = message.getText();
        Long telegrmUserId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        if (messageText.trim().toLowerCase().contains("да")){
            AppUser user = AppUser.builder().telegramUserId(telegrmUserId)
                    .firstname(message.getChat().getFirstName())
                    .username(message.getChat().getUserName())
                    .build();
            appUserService.saveUser(user);
            sendMessage.setText("""
                Теперь можем продолжить работу.
                Для просмотра функций бота обратитесь к помощи бота"
                "Напишите: Что умеет бот?""");
        }else {
            sendMessage.setText("""
                    Без вашего подтверждения на то чтобы я Вас запомнил,
                    к сожалению, я не смогу продожить работу с вами.
                    Если все же передумаете я Вас всегда жду.
                    """);
        }
        mapUser.remove(telegrmUserId);
        return Pair.of(sendMessage, "non_sticker");
    }
}
