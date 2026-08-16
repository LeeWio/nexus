package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.payload.response.GitHubActivityResponse;
import space.nebula.nexus.service.IGitHubService;

import java.time.YearMonth;
import java.time.ZoneOffset;

@Tag(name = "Public GitHub", description = "Public endpoints for GitHub activity and stats")
@RestController
@RequestMapping("/api/v1/public/github")
@RequiredArgsConstructor
public class PublicGitHubController {

	private final IGitHubService githubService;

	@Operation(summary = "Retrieve public GitHub profile statistics", description = "Return the latest synchronized GitHub profile and repository metrics for public profile widgets. Values may be cached between scheduled synchronizations.")
	@GetMapping("/stats")
	public ApiResponse<GitHubStatsResponse> retrieveStats() {
		return ApiResponse.success(githubService.retrieveGlobalStats());
	}

	@Operation(summary = "Retrieve current public GitHub activity", description = "Return public commit, pull request, issue, and review contributions for the current UTC month. Private repository details are never exposed. Values are cached for one hour.")
	@GetMapping("/activity")
	public ApiResponse<GitHubActivityResponse> retrieveActivity() {
		return ApiResponse.success(githubService.retrieveActivity(YearMonth.now(ZoneOffset.UTC)));
	}
}
