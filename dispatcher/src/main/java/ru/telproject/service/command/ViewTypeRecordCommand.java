package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.exception.UserNotFoundException;
import ru.telproject.service.AppUserService;
import ru.telproject.service.TypeRecordingService;
import ru.telproject.service.custom_interface.Command;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewTypeRecordCommand implements Command {
    private final TypeRecordingService typeRecordingService;
    private final AppUserService appUserService;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        log.info("Processing view record type first message for chat ID: {}", message.getText());
        Long telegramUserId = message.getChatId();
        AppUser appUser = appUserService.findAppUserByTelegramId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        List<TypeRecording> allTypeRecordUser = typeRecordingService.findAllByAppUserId(appUser.getId());
        String formatStringTypesRecord = getFormatStringTypesRecord(allTypeRecordUser);
        SendMessage sendMessage = new SendMessage();
        if (StringUtils.hasText(formatStringTypesRecord)) {
            sendMessage.setText("Перечень ваших услуг:\n" + formatStringTypesRecord);
        }else {
            sendMessage.setText("У вас пока нет созданных услуг");
        }
        log.info("Successfully view record type first message for chat ID: {}", message.getText());
        return sendMessage;
    }

    private String getFormatStringTypesRecord(List<TypeRecording> typeRecordings){
        String formatString = typeRecordings.stream()
                .map(t -> "Имя услуги: " + t.getTypeName() + ", цена услуги: " + t.getTypeCoast())
                .collect(Collectors.joining("\n"));
        return formatString;
    }
}
