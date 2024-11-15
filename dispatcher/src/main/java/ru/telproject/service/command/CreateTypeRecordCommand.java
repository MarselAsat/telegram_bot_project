package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CreateTypeRecordCommand implements Command {
    private final ConcurrentHashMap<Long, String> userData = new ConcurrentHashMap<>();

    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        String text = message.getText();
        Pattern pattern = Pattern.compile("(услугу|услуги)\\s+(\\p{L}+)", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        String typeName = "";
        while (matcher.find()) {
            typeName = matcher.group(2);
            userData.put(message.getChat().getId(), typeName);
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(String.format("Какая цена будет у услуги %s?", typeName));
        return sendMessage;
    }

    @Override
    public Pair<SendMessage, String> executeNextMessage(Message message) {
        String messageText = message.getText();
        Pattern pattern = Pattern.compile("\\b(\\d+.\\d+|\\d+)");
        Matcher matcher = pattern.matcher(messageText);
        SendMessage sendMessage = new SendMessage();
        Long userId = message.getChatId();
        if (matcher.find()) {
            double price = Double.parseDouble(matcher.group());
            TypeRecording typeRecording = new TypeRecording();
            String typeName = userData.get(userId);
            Optional<AppUser> telegramUserId = appUserService.findAppUserByTelegramId(userId);
            List<TypeRecording> byTypeNameIgnoreCaseAndAppUserId = typeRecordingService
                    .findByTypeNameIgnoreCaseAndAppUserId(typeName, telegramUserId.orElseThrow().getId());
            if (byTypeNameIgnoreCaseAndAppUserId.size() == 0) {
                typeRecording.setTypeName(typeName);
                typeRecording.setTypeCoast(price);
                Optional<AppUser> byTelegramUserId = appUserService.findAppUserByTelegramId(userId);
                typeRecording.setAppUser(byTelegramUserId.orElseThrow());
                typeRecordingService.saveTypeRecord(typeRecording);
                sendMessage.setText(String.format("Создал услугу: %s \n цена услуги: %s", typeName, price));
                return Pair.of(sendMessage, "classpath:sticker/yes-sir.webm");
            } else {
                sendMessage.setText(String.format("У вас уже есть такая услуга: %s \nНе получится сохранить",
                        byTypeNameIgnoreCaseAndAppUserId.get(0).getTypeName()));
                return Pair.of(sendMessage, "classpath:sticker/no-no-no.webm");
            }
        } else {
            sendMessage.setText("Не смог понять цену которую вы указали, попробуйте еще раз указать цену");
            userData.remove(userId);
            return Pair.of(sendMessage, "classpath:dont_understand.webm");
        }
    }
}
