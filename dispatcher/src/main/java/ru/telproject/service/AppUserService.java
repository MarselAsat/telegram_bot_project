package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.telproject.entity.AppUser;
import ru.telproject.repository.AppUserRepository;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, AppUser> hashOperations;
    private final AppUserRepository appUserRepository;
    private final String APP_USER_HASH_KEY = "AppUser";

    @PostConstruct
    private void init(){
        hashOperations = redisTemplate.opsForHash();
    }

    public void saveUser(AppUser appUser){
        appUserRepository.save(appUser);
        hashOperations.put(APP_USER_HASH_KEY, appUser.getTelegramUserId().toString(), appUser);
    }

    public Optional<AppUser> findAppUserByTelegramId(Long id){
        AppUser appUser = hashOperations.get(APP_USER_HASH_KEY, id.toString());
        if (appUser == null){
            appUser = appUserRepository.findByTelegramUserId(id).orElse(null);
            if (appUser != null){
                hashOperations.put(APP_USER_HASH_KEY, appUser.getTelegramUserId().toString(), appUser);
            }
        }
        return Optional.ofNullable(appUser);
    }

    public Map<String, AppUser> findAllAppUser(){
        return hashOperations.entries(APP_USER_HASH_KEY);
    }

    public void deleteAppUser(Long telegramId){
        appUserRepository.deleteAppUserByTelegramUserId(telegramId);
        hashOperations.delete(APP_USER_HASH_KEY, telegramId);
    }

}
