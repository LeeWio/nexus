package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.FriendLinkStatus;

@Schema(description = "Friend Link creation/update or application request")
public record FriendLinkRequest(
    @Schema(description = "Site name", example = "Nebula Space")
    @NotBlank(message = "Site name is required")
    @Size(max = 100)
    String name,

    @Schema(description = "Site URL", example = "https://nebula.space")
    @NotBlank(message = "URL is required")
    @Size(max = 255)
    String url,

    @Schema(description = "Site avatar URL")
    String avatar,

    @Schema(description = "Site description")
    @Size(max = 500)
    String description,

    @Schema(description = "Contact email of the applicant", example = "owner@example.com")
    @Email(message = "Invalid email format")
    String email,

    @Schema(description = "Display order")
    Integer sortOrder,

    @Schema(description = "Whether the link is publicly visible")
    Boolean isPublished,

    @Schema(description = "Moderation status (Managed by Admin)")
    FriendLinkStatus status
) {}
