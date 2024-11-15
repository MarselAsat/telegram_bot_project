package ru.telproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long telegramUserId;

    private String username;

    private String firstname;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "appUser", fetch = FetchType.LAZY)
    private List<TypeRecording> typeRecordings = new ArrayList<>();

//    @Builder.Default
//    @OneToOne(mappedBy = "appUser", fetch = FetchType.LAZY)
//    private List<RecordingUser> userRecordings = new ArrayList<>();

}
