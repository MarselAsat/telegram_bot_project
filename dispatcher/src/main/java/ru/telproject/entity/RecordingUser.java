package ru.telproject.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recording_user")
public class RecordingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type_recording_id")
    private TypeRecording typeRecording;

    @ManyToOne
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    private LocalDateTime recordingTime;
}
