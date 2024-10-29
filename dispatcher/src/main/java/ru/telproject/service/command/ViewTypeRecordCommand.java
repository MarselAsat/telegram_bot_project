package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
    @Override
    public SendMessage executeFirstMessage(Message message) {
        log.info("Processing view record type first message for chat ID: {}", message.getText());
        Long telegramUserId = message.getChatId();
        Optional<AppUser> byTelegramUserId = appUserRepository.findByTelegramUserId(telegramUserId);
        AppUser appUser = byTelegramUserId.orElseThrow();
        List<TypeRecording> allTypeRecordUser = typeRecordingService.findAllByAppUserId(appUser.getId());
        String formatStringTypesRecord = getFormatStringTypesRecord(allTypeRecordUser);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Перечень ваших услуг:\n" + formatStringTypesRecord);
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
