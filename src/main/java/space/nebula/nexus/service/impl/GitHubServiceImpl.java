package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Dict;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.service.IGitHubService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements IGitHubService
{

	private final RestClient restClient;
	private final RedisUtil redisUtil;
	private final ProjectRepository projectRepository;
	private final Executor asyncExecutor;

	@Value("${app.github.username}")
	private String githubUsername;

	@Value("${app.github.token:}")
	private String githubToken;

	private static final String GITHUB_API_BASE = "https://api.github.com";

	@Override
	@CircuitBreaker(name = "githubService", fallbackMethod = "fallbackStats")
	@Retry(name = "githubService")
	public GitHubStatsResponse retrieveGlobalStats()
	{
		return redisUtil.get(CacheConstants.GITHUB_STATS_CACHE_KEY, GitHubStatsResponse.class).orElseGet(() ->
		{
			log.info("GitHub global stats cache miss, fetching from API...");
			GitHubStatsResponse stats = fetchGlobalStatsFromApi();
			if (stats != null)
			{
				redisUtil.set(CacheConstants.GITHUB_STATS_CACHE_KEY, stats, 6, TimeUnit.HOURS);
			}
			return stats;
		});
	}

	@Override
	@CircuitBreaker(name = "githubService", fallbackMethod = "fallbackMetrics")
	@Retry(name = "githubService")
	public Map<String, Object> retrieveRepoMetrics(String repoName)
	{
		Map<String, Object> body = restClient.get()
				.uri(GITHUB_API_BASE + "/repos/{owner}/{repo}", githubUsername, repoName)
				.accept(MediaType.parseMediaType("application/vnd.github.v3+json")).headers(headers ->
				{
					if (githubToken != null && !githubToken.isBlank())
					{
						headers.setBearerAuth(githubToken);
					}
				}).retrieve().body(Map.class);

		if (body != null)
		{
			return Dict.create()
					.set("stars", body.get("stargazers_count"))
					.set("forks", body.get("forks_count"))
					.set("language", body.get("language"));
		}
		return null;
	}

	@Override
	public void synchronizeProjectMetrics()
	{
		log.info("Starting parallel synchronization of GitHub project metrics...");
		List<Project> projects = projectRepository.findAll();

		List<CompletableFuture<Project>> futures = projects.stream().map(project -> CompletableFuture.supplyAsync(() ->
		{
			if (project.getGithubUrl() != null && !project.getGithubUrl().isBlank())
			{
				String repoName = extractRepoName(project.getGithubUrl());
				if (repoName != null)
				{
					Map<String, Object> metrics = retrieveRepoMetrics(repoName);
					if (metrics != null)
					{
						project.setStarsCount((Integer) metrics.get("stars"));
						project.setForksCount((Integer) metrics.get("forks"));
						project.setLanguage((String) metrics.get("language"));
						project.setRepoName(repoName);
						return project;
					}
				}
			}
			return null;
		}, asyncExecutor)).toList();

		List<Project> updatedProjects = futures.stream().map(CompletableFuture::join).filter(java.util.Objects::nonNull)
				.toList();

		if (!updatedProjects.isEmpty())
		{
			projectRepository.saveAll(updatedProjects);
		}

		log.info("GitHub project metrics synchronization complete. Total updated: {}", updatedProjects.size());

		// Also refresh global stats
		redisUtil.delete(CacheConstants.GITHUB_STATS_CACHE_KEY);
	}

	private GitHubStatsResponse fetchGlobalStatsFromApi()
	{
		Map body = restClient.get().uri(GITHUB_API_BASE + "/users/{username}", githubUsername)
				.accept(MediaType.parseMediaType("application/vnd.github.v3+json")).headers(headers ->
				{
					if (githubToken != null && !githubToken.isBlank())
					{
						headers.setBearerAuth(githubToken);
					}
				}).retrieve().body(Map.class);

		if (body != null)
		{
			return GitHubStatsResponse.builder().followers((Integer) body.get("followers"))
					.publicRepos((Integer) body.get("public_repos")).htmlUrl((String) body.get("html_url"))
					.avatarUrl((String) body.get("avatar_url")).totalStars(0).build();
		}
		return null;
	}

	public GitHubStatsResponse fallbackStats(Exception e)
	{
		log.error("GitHub stats fallback triggered due to: {}", e.getMessage());
		return GitHubStatsResponse.builder().followers(0).publicRepos(0).totalStars(0)
				.htmlUrl("https://github.com/" + githubUsername).build();
	}

	public Map<String, Object> fallbackMetrics(String repoName, Exception e)
	{
		log.error("GitHub metrics fallback triggered for {} due to: {}", repoName, e.getMessage());
		return Dict.create()
				.set("stars", 0)
				.set("forks", 0)
				.set("language", "Unknown");
	}

	private String extractRepoName(String githubUrl)
	{
		try
		{
			String[] parts = githubUrl.split("/");
			if (parts.length >= 5)
			{
				return parts[parts.length - 1].replace(".git", "");
			}
		}
		catch (Exception e)
		{
			log.warn("Could not extract repo name from URL: {}", githubUrl);
		}
		return null;
	}
}
