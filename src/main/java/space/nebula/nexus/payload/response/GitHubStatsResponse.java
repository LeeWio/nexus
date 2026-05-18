package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;

@Builder
@Schema(description = "Global GitHub statistics for the user profile")
public record GitHubStatsResponse(
    @Schema(description = "Total number of followers", example = "100")
    Integer followers,
    
    @Schema(description = "Total number of public repositories", example = "50")
    Integer publicRepos,
    
    @Schema(description = "Total star count across all repositories (if syncable)", example = "1000")
    Integer totalStars,
    
    @Schema(description = "GitHub profile URL", example = "https://github.com/LeeWio")
    String htmlUrl,
    
    @Schema(description = "User avatar URL")
    String avatarUrl
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
