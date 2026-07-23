package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public friend-link application fields controlled by the applicant. */
@Schema(description = "Public friend-link application")
public record FriendLinkApplicationRequest(@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 255) String url, @Size(max = 255) String avatar, @Size(max = 500) String description,
		@NotBlank @Email @Size(max = 100) String email) {
}
