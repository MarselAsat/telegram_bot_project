package ru.telproject.service.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.service.custom_interface.Command;

@Component
@Slf4j
public class InfoViewCommand implements Command {
    @Override
    public SendMessage executeFirstMessage(Message message) {
        log.info("Processing get info message for chat ID: {}", message.getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("""
                Я бот-помощник мастерам предосталяющим услуги.
                С моей помощью вы можете вести свои записи.
                Вот что я могу:
                    - Создать тип услуги; -
                    - Создать новую запись с типом услуги -
                    - Удалить созданную вами запись на услугу -
                    - Редактировать вашу запись -
                    - Редактировать вашу услугу -
                    - Удалить услугу -
                    - Показать ваши записи на конкертное число
                Пример как создавать услугу:
                    Напишите: Хочу создать услугу маникюр
                    После чего я запрошу у вас цену услуги
                Пример как создать запись:
                    Напишите: Создай запись на маникюр в 16:00 15 ноября
                Пример как посмотреть ваши записи:
                    Напишите: Покажи мне мои услуги
                Пример как удалить созданную вами запись:
                    Напишите : Удали запись на маникюр в 16:00 15 ноября
                Пример как редактировать вашу запись:
                    Напишите: Измени время записи на маникюр 15 ноября с 16:00 на 16 ноября в 19:00
                Пример как редактировать вашу услугу:
                    Напишите: Поменяй название услуги маникюр на педикюр
                    или
                    Напишите: Поменяй цену услуги маникюр на 3000
                Пример как удалить услугу:
                    Напишите: Удали услугу маникюр
                Пример как посмотреть записи на конкретное число:
                    Напишите: Покажи записи на 16 ноября
                """);
        return sendMessage;
    }
}
