package telproject.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.persistence.*;

@EqualsAndHashCode(of = "event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class RawData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Update event;


}
