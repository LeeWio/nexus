package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchDeleteRequest(
    @NotEmpty(message = "IDs cannot be empty")
    List<Long> ids
) {}
