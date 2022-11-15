package ru.telproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.telproject.entity.AppUser;
import ru.telproject.entity.TypeRecording;

import java.util.List;
import java.util.Optional;

public interface TypeRecordingRepository extends JpaRepository<TypeRecording, Long> {
    List<TypeRecording> findByTypeNameIgnoreCaseAndAppUserId(String typeName, Long appUserId);
    List<TypeRecording> findAllByAppUserId(Long appUserId);
}
