package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for autosaving post content")
public record PostAutosaveRequest(
    @Schema(description = "Post ID (for existing posts) or a client-generated UUID (for new posts)", example = "1")
    @NotBlank(message = "Identifier is required for autosave")
    String identifier,

    @Schema(description = "In-progress content being saved")
    @NotBlank(message = "Content cannot be empty")
    String content
) {}
