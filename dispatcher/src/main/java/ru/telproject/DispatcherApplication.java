package ru.telproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.telproj.user_hash.RedisConfig;
import ru.telproject.service.RedisKeyExpirationListener;

@SpringBootApplication
@Import(RedisConfig.class)
@EnableScheduling
public class DispatcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(DispatcherApplication.class);
    }
    @Bean
    MessageListenerAdapter listenerAdapter(RedisKeyExpirationListener listener) {
        return new MessageListenerAdapter(listener);
    }

}
