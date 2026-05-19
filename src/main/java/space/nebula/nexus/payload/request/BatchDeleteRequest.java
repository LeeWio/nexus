package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request for batch deleting entities by ID")
public record BatchDeleteRequest(
    @Schema(description = "List of unique identifiers to delete", example = "[10, 11, 12]")
    @NotEmpty(message = "IDs cannot be empty")
    List<Long> ids
) {}
