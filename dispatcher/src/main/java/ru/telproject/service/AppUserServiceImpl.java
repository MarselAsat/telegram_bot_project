package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.entity.AppUser;
import ru.telproject.repository.AppUserRepository;
import ru.telproject.service.interfac.AppUserService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;

    @Override
    public AppUser createUser(Update update) {
        AppUser appUser = AppUser.builder()
                .telegramUserId(update.getMessage().getFrom().getId())
                .username(update.getMessage().getFrom().getUserName())
                .firstname(update.getMessage().getFrom().getFirstName())
                .build();
        AppUser save = appUserRepository.save(appUser);
        System.out.println("SAVE - " + appUser);
        return save;
    }

    @Override
    public Optional<AppUser> findByTelegramId(Update update) {
        return appUserRepository
                .findByTelegramUserId(update.getMessage().getFrom().getId());
    }
}
