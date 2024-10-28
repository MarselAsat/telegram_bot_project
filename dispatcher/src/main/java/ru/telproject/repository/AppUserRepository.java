package ru.telproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.telproject.entity.AppUser;

import java.util.Optional;

public interface AppUserRepository  extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTelegramUserId(Long telegramUserId);
    void deleteAppUserByTelegramUserId(Long telegramId);
}
