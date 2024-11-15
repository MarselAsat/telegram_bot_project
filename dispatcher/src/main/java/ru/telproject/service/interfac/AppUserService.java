package ru.telproject.service.interfac;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.entity.AppUser;

import java.util.Optional;

public interface AppUserService {
    AppUser createUser(Update update);
    Optional<AppUser> findByTelegramId(Update update);
}
