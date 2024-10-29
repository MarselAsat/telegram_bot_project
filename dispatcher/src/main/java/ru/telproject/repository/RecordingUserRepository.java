package ru.telproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.telproject.entity.RecordingUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecordingUserRepository extends JpaRepository<RecordingUser, Long> {
List<RecordingUser> findByAppUserIdAndRecordingTime(Long appUserId, LocalDateTime dateTime);
@Query("SELECT r FROM RecordingUser r " +
        "WHERE r.appUser.id = :appUserId " +
        "AND (:startTime IS NULL OR r.recordingTime >= :startTime) AND " +
        "(:endTime IS NULL OR r.recordingTime <= :endTime)")
List<RecordingUser> findByAppUserIdAndTimeBetween(Long appUserId, LocalDateTime startTime, LocalDateTime endTime);


List<RecordingUser> findAllByRecordingTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

@Query("SELECT r FROM RecordingUser r " +
        "WHERE r.recordingTime = :recordTime " +
        "AND r.typeRecording.id = :typeId " +
        "AND r.appUser.id = :appUserId")
Optional<RecordingUser> findRecordsByTimeTypeIdUserID(LocalDateTime recordTime, Long typeId, Long appUserId);
}
