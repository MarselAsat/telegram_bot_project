package ru.telproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.telproject.entity.RecordingUser;
import ru.telproject.model.RabbitQueue;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
@Service
public class RedisKeyExpirationListener implements MessageListener {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        RecordingUser recordingUser = objectMapper.convertValue(redisTemplate.opsForValue().getAndDelete("shadow:" + key), RecordingUser.class);
        rabbitTemplate.convertAndSend(RabbitQueue.RECORD_USER_PUSH, recordingUser);
    }
}
