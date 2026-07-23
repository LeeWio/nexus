package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Comment update request")
public record CommentUpdateRequest(
		@Schema(description = "Updated markdown or plain text content", example = "I refined my thought here.") @NotBlank(message = "Comment content is required") @Size(max = 1000, message = "Comment content must not exceed 1000 characters") String content) {
}
