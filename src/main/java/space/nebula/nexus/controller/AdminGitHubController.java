package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IGitHubService;

@Tag(name = "Admin GitHub", description = "Admin endpoints for GitHub synchronization")
@RestController
@RequestMapping("/api/v1/admin/github")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGitHubController {

	private final IGitHubService githubService;

	@Operation(summary = "Trigger GitHub metrics synchronization", description = "Refresh configured GitHub project metrics on demand. Use this after updating repository metadata when the scheduled synchronization should not be awaited.")
	@PostMapping("/sync")
	public ApiResponse<Void> triggerSync() {
		githubService.synchronizeProjectMetrics();
		return ApiResponse.success("GitHub synchronization triggered successfully", null);
	}
}
