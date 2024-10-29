package ru.telproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telproject.controller.UpdateController;
import ru.telproject.entity.RecordingUser;

import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Service
public class RedisKeyExpirationListener implements MessageListener {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UpdateController updateController;
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        RecordingUser recordingUser = objectMapper.convertValue(redisTemplate.opsForValue().getAndDelete("shadow:" + key), RecordingUser.class);
        SendMessage sendMessage = new SendMessage();
        String text =recordingUser.getAppUser().getFirstname() + "\nВаша предстоящая запись:\n- " +
                recordingUser.getTypeRecording().getTypeName() + "\nВремя записи: " +
                recordingUser.getRecordingTime().format(DateTimeFormatter.ofPattern("yyy-dd-MM HH:mm"));
        sendMessage.setText(text);
        sendMessage.setChatId(recordingUser.getAppUser().getTelegramUserId());
        updateController.sendMessage(sendMessage);
    }
}
