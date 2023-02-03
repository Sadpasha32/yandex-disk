package disk;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "systemitem")
@RequiredArgsConstructor
public class SystemItem {
    @Id
    String id;
    @Nullable
    String url;
    String date;
    @Nullable
    String parentId;
    Type type;
    @Nullable
    Integer size;
    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true)
    @Nullable
    List<SystemItem> children;

    public enum Type {
        FILE,FOLDER
    }
}
