package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;
import ru.telproject.repository.AppUserRepository;
import ru.telproject.repository.TypeRecordingRepository;
import ru.telproject.service.interfac.TypeRecordingService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeRecordingServiceImpl implements TypeRecordingService {

    private final TypeRecordingRepository typeRecordingRepository;
    private final AppUserRepository appUserRepository;
    @Override
    public List<TypeRecording> findByTypeNameIgnoreCase(String typeName, Long appUserTelegramId) {
        Optional<AppUser> appUser = appUserRepository.findByTelegramUserId(appUserTelegramId);

        List<TypeRecording> maybeType = typeRecordingRepository.findByTypeNameIgnoreCaseAndAppUserId(typeName, appUser.get().getId());

        return maybeType;
    }

    @Override
    public TypeRecording createType(String typeName, Double coast, Long appUserId) {
        TypeRecording type = TypeRecording.builder()
                .appUser(appUserRepository.findByTelegramUserId(appUserId).get())
                .typeName(typeName)
                .typeCoast(coast)
                .build();
        TypeRecording save = typeRecordingRepository.save(type);
        return save;
    }

    @Override
    public List<TypeRecording> findAllType(Long appUserId) {
        Optional<AppUser> byTelegramUserId = appUserRepository.findByTelegramUserId(appUserId);
        List<TypeRecording> allType = typeRecordingRepository.findAllByAppUserId(byTelegramUserId.get().getId());
        return allType;
    }

    @Override
    public void deleteType(Long typeId) {
        typeRecordingRepository.deleteById(typeId);
    }
}
