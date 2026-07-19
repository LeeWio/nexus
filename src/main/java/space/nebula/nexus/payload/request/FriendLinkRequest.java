package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.FriendLinkStatus;

@Schema(description = "Friend Link creation/update or application request")
public record FriendLinkRequest(
		@Schema(description = "Site name", example = "Nebula Space") @NotBlank(message = "Site name is required") @Size(min = 1, max = 100, message = "Site name must be between 1 and 100 characters") String name,

		@Schema(description = "Site URL", example = "https://nebula.space") @NotBlank(message = "URL is required") @Size(min = 1, max = 255, message = "URL must be between 1 and 255 characters") String url,

		@Schema(description = "Site avatar/logo URL", example = "https://nebula.space/logo.png") @Size(max = 255) String avatar,

		@Schema(description = "Brief site description", example = "A blog about technology and space.") @Size(max = 500) String description,

		@Schema(description = "Contact email of the owner/applicant", example = "owner@example.com") @Email(message = "Email address is invalid") @Size(max = 100) String email,

		@Schema(description = "Display priority (lower value means higher priority)", example = "10") Integer sortOrder,

		@Schema(description = "Whether the link is active and visible", example = "true") Boolean isPublished,

		@Schema(description = "Current moderation status", example = "APPROVED") FriendLinkStatus status) {
}
