package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.service.IGitHubService;

@Tag(name = "Public GitHub", description = "Public endpoints for GitHub activity and stats")
@RestController
@RequestMapping("/api/v1/public/github")
@RequiredArgsConstructor
public class PublicGitHubController
{

	private final IGitHubService githubService;

	@Operation(summary = "Retrieve global GitHub statistics for the profile")
	@GetMapping("/stats")
	public ApiResponse<GitHubStatsResponse> retrieveStats()
	{
		return ApiResponse.success(githubService.retrieveGlobalStats());
	}
}
