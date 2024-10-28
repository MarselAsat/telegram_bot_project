package ru.telproject.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import ru.telproject.entity.RecordingUser;
import ru.telproject.repository.RecordingUserRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ScheduledTask implements Job {

    private final RecordingUserRepository recordingUserRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static String RECORDING_HASH_KEY = "RecordingUser";
    private ValueOperations<String, Object> valueOperations;

    @PostConstruct
    private void init(){
        valueOperations = redisTemplate.opsForValue();
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime endTime = startTime.plusHours(2);
        List<RecordingUser> allRecordingOnNextTwoHour = recordingUserRepository.findAllByRecordingTimeBetween(startTime, endTime);
        for (RecordingUser recordingUser : allRecordingOnNextTwoHour){
            String key = RECORDING_HASH_KEY + recordingUser.getId();
            long differenceSecond = startTime.until(recordingUser.getRecordingTime().minusHours(1), ChronoUnit.SECONDS);
            System.out.println(differenceSecond);
            if (differenceSecond > 0) {
                valueOperations.set(key, recordingUser);
                valueOperations.set("shadow:" + key, recordingUser);
                redisTemplate.expire(key, differenceSecond, TimeUnit.SECONDS);
            }
        }
    }

}
