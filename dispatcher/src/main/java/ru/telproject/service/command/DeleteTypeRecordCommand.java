package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DeleteTypeRecordCommand implements Command {
    private final AppUserService appUserService;
    private final TypeRecordingService typeRecordingService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        String text = message.getText();
        Long telegramUserId = message.getChatId();
        String typeName = "";
        SendMessage sendMessage = new SendMessage();
        Pattern pattern = Pattern.compile("(услугу|услуги)\\s+(\\p{L}+)", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            typeName = matcher.group(2);
        }
        Optional<AppUser> byTelegramUserId = appUserService.findAppUserByTelegramId(telegramUserId);
        List<TypeRecording> typeRecordings = typeRecordingService.findByTypeNameIgnoreCaseAndAppUserId(typeName, byTelegramUserId.orElseThrow().getId());
        if (typeRecordings.size() == 0) {
            sendMessage.setText(String.format("Не смог удалить тип услуги: %s, потому что не нашел у вас такой услуги", typeName));
            return sendMessage;
        } else {
            typeRecordingService.delete(typeRecordings.get(0));
            sendMessage.setText(String.format("Удалил услугу: %s, стоимостью: %s", typeRecordings.get(0).getTypeName(),
                    typeRecordings.get(0).getTypeCoast()));
            return sendMessage;
        }
    }
}
