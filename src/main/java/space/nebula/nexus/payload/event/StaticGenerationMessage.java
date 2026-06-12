package space.nebula.nexus.payload.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticGenerationMessage implements Serializable {
    public enum Action {
        GENERATE, DELETE
    }

    private Long postId;
    private String slug;
    private Action action;
}
