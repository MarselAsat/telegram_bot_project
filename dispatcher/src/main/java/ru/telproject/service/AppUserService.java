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
    private final MetricsService metricsService;
    private final String APP_USER_HASH_KEY = "AppUser";

    @PostConstruct
    private void init(){
        hashOperations = redisTemplate.opsForHash();
    }

    public void saveUser(AppUser appUser){
        metricsService.recordingTimeVoid("app_user_save_time", () -> {
            appUserRepository.save(appUser);
            hashOperations.put(APP_USER_HASH_KEY, appUser.getTelegramUserId().toString(), appUser);
            metricsService.incrementCounter("app_user_save_successful_counter",
                    "user_id", appUser.getTelegramUserId().toString());
        });
    }

    public Optional<AppUser> findAppUserByTelegramId(Long id){
       return metricsService.recordingTime("app_user_find_time", () -> {
            AppUser appUser = hashOperations.get(APP_USER_HASH_KEY, id.toString());
            if (appUser == null) {
                appUser = appUserRepository.findByTelegramUserId(id).orElse(null);
                if (appUser != null) {
                    hashOperations.put(APP_USER_HASH_KEY, appUser.getTelegramUserId().toString(), appUser);
                }
            }
            metricsService.incrementCounter("app_user_find_successful_counter", "user_id",
                    appUser.getTelegramUserId().toString());
            return Optional.ofNullable(appUser);
        });
    }


}
