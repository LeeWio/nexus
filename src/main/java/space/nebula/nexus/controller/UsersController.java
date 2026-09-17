package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.response.UserMentionResponse;
import space.nebula.nexus.service.IUserService;

/**
 * Authenticated user directory endpoints that return public-safe summaries.
 */
@Tag(name = "Users", description = "Authenticated user directory lookups for editor features such as @mentions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

	private final IUserService userService;

	@GetMapping("/mentions")
	@Operation(summary = "Search mentionable users", description = "Returns active users matching username or nickname. Response includes only id, username, nickname, and avatar — never email or roles. Requires authentication.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mention candidates", content = @Content(schema = @Schema(implementation = UserMentionResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")})
	@RateLimit(count = 60, time = 1, unit = TimeUnit.MINUTES, message = "Too many mention searches. Please slow down.")
	public ApiResponse<List<UserMentionResponse>> searchMentions(
			@Parameter(description = "Username or nickname keyword; empty returns a small active-user sample") @RequestParam(required = false, defaultValue = "") String q,
			@Parameter(description = "Max results (1-50, default 20)") @RequestParam(required = false, defaultValue = "20") int limit) {
		return userService.searchMentionableUsers(q, limit);
	}
}
