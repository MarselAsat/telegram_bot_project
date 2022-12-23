package ru.telproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.telproject.entity.RecordingUser;

import java.time.LocalDate;
import java.util.List;

public interface RecordingUserRepository extends JpaRepository<RecordingUser, Long> {
List<RecordingUser> findByAppUserIdAndRecordingTime(Long appUserId, LocalDate recordingDate);
}
