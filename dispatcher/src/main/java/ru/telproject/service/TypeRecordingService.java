package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.TypeRecordingRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeRecordingService {
    private final TypeRecordingRepository typeRecordingRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, TypeRecording> hashOperations;
    private final MetricsService metricsService;
    private SetOperations<String,Object> setOperations;
    private final String TYPE_RECORDING_HASH_KEY = "TypeRecording";
    private final String INDEX_KEY_PREFIX = "TypeRecordingIndex:";
    private final String USER_INDEX_KEY_PREFIX = "UserTypeRecordingIndex:";

    @PostConstruct
    private void init(){
        hashOperations = redisTemplate.opsForHash();
        setOperations = redisTemplate.opsForSet();
    }

    public void saveTypeRecord(TypeRecording typeRecording){
        metricsService.recordingTimeVoid("type_record_save_time", () -> {
            typeRecordingRepository.save(typeRecording);
            hashOperations.put(TYPE_RECORDING_HASH_KEY, typeRecording.getId().toString(), typeRecording);
            setOperations.add(INDEX_KEY_PREFIX + typeRecording.getAppUser().getId(), typeRecording.getId().toString());
            setOperations.add(USER_INDEX_KEY_PREFIX + typeRecording.getAppUser().getId(), typeRecording.getId().toString());
            metricsService.incrementCounter("type_record_save_counter");
        });
    }

    public List<TypeRecording> findByTypeNameIgnoreCaseAndAppUserId(String typeName, Long appUserId){
       return metricsService.recordingTime("type_record_find_typename_time", () -> {
            Set<Object> ids = setOperations.members(INDEX_KEY_PREFIX + typeName.toLowerCase() + ":" + appUserId);
            List<TypeRecording> typeRecordingList = ids.stream()
                    .map(id -> hashOperations.get(TYPE_RECORDING_HASH_KEY, id))
                    .collect(Collectors.toList());
            if (typeRecordingList.isEmpty()) {
                typeRecordingList = typeRecordingRepository.findByTypeNameIgnoreCaseAndAppUserId(typeName, appUserId)
                        .stream().peek(t -> t.getAppUser()).collect(Collectors.toList());
                typeRecordingList.stream().peek(t -> saveTypeRecordHash(t)).count();
            }
            metricsService.incrementCounter("type_record_find_typename_counter");
            return typeRecordingList;
        });
    }

    public List<TypeRecording> findAllByAppUserId(Long appUserId){
        return metricsService.recordingTime("type_record_find_all_time", () -> {
            Set<Object> ids = setOperations.members(USER_INDEX_KEY_PREFIX + appUserId);
            List<TypeRecording> typeRecordingList = ids.stream()
                    .map(id -> hashOperations.get(TYPE_RECORDING_HASH_KEY, id))
                    .collect(Collectors.toList());
            if (typeRecordingList.isEmpty()) {
                typeRecordingList = typeRecordingRepository.findAllByAppUserId(appUserId);
                typeRecordingList.forEach(t -> saveTypeRecordHash(t));
            }
            metricsService.incrementCounter("type_record_find_all_counter");
            return typeRecordingList;
        });
    }

    public void delete(TypeRecording typeRecording){
        metricsService.recordingTimeVoid("type_record_delete_time", () -> {
            typeRecordingRepository.delete(typeRecording);
            TypeRecording typeRecordingHash = hashOperations.get(TYPE_RECORDING_HASH_KEY, typeRecording.getId().toString());
            if (typeRecordingHash != null) {
                setOperations.remove(INDEX_KEY_PREFIX + typeRecordingHash.getTypeName().toLowerCase() + ":" +
                        typeRecordingHash.getAppUser().getId(), typeRecordingHash.getId().toString());
                setOperations.remove(USER_INDEX_KEY_PREFIX + typeRecordingHash.getAppUser().getId(),
                        typeRecordingHash.getId().toString());
                hashOperations.delete(TYPE_RECORDING_HASH_KEY, typeRecordingHash.getId().toString());
            }
            metricsService.incrementCounter("type_record_delete_counter");
        });
    }

    public void saveTypeRecordHash(TypeRecording typeRecording){
        hashOperations.put(TYPE_RECORDING_HASH_KEY, typeRecording.getId().toString(), typeRecording);
        setOperations.add(INDEX_KEY_PREFIX + typeRecording.getAppUser().getId(), typeRecording.getId().toString());
        setOperations.add(USER_INDEX_KEY_PREFIX + typeRecording.getAppUser().getId(), typeRecording.getId().toString());
    }


}
