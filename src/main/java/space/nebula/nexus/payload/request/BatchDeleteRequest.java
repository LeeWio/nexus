package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request for batch deleting entities by ID")
public record BatchDeleteRequest(
		@Schema(description = "List of unique identifiers to delete", example = "[10, 11, 12]") @NotEmpty(message = "IDs are required") @Size(max = 100, message = "At most 100 IDs can be deleted at once") List<@NotNull(message = "ID cannot be null") Long> ids) {
}
