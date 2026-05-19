package space.nebula.nexus.service.impl;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements IGitHubService {

    private final RestClient restClient;
    private final RedisUtil redisUtil;
    private final ProjectRepository projectRepository;

    @Value("${app.github.username}")
    private String githubUsername;

    @Value("${app.github.token:}")
    private String githubToken;

    private static final String GITHUB_API_BASE = "https://api.github.com";

    @Override
    @CircuitBreaker(name = "githubService", fallbackMethod = "fallbackStats")
    @Retry(name = "githubService")
    public GitHubStatsResponse retrieveGlobalStats() {
        return redisUtil.get(CacheConstants.GITHUB_STATS_CACHE_KEY, GitHubStatsResponse.class)
                .orElseGet(() -> {
                    log.info("GitHub global stats cache miss, fetching from API...");
                    GitHubStatsResponse stats = fetchGlobalStatsFromApi();
                    if (stats != null) {
                        redisUtil.set(CacheConstants.GITHUB_STATS_CACHE_KEY, stats, 6, TimeUnit.HOURS);
                    }
                    return stats;
                });
    }

    @Override
    @CircuitBreaker(name = "githubService", fallbackMethod = "fallbackMetrics")
    @Retry(name = "githubService")
    public Map<String, Object> retrieveRepoMetrics(String repoName) {
        Map<String, Object> body = restClient.get()
                .uri(GITHUB_API_BASE + "/repos/{owner}/{repo}", githubUsername, repoName)
                .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
                .headers(headers -> {
                    if (githubToken != null && !githubToken.isBlank()) {
                        headers.setBearerAuth(githubToken);
                    }
                })
                .retrieve()
                .body(Map.class);

        if (body != null) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("stars", body.get("stargazers_count"));
            metrics.put("forks", body.get("forks_count"));
            metrics.put("language", body.get("language"));
            return metrics;
        }
        return null;
    }

    @Override
    public void synchronizeProjectMetrics() {
        log.info("Starting synchronization of GitHub project metrics...");
        List<Project> projects = projectRepository.findAll();
        int updatedCount = 0;

        for (Project project : projects) {
            if (project.getGithubUrl() != null && !project.getGithubUrl().isBlank()) {
                String repoName = extractRepoName(project.getGithubUrl());
                if (repoName != null) {
                    Map<String, Object> metrics = retrieveRepoMetrics(repoName);
                    if (metrics != null) {
                        project.setStarsCount((Integer) metrics.get("stars"));
                        project.setForksCount((Integer) metrics.get("forks"));
                        project.setLanguage((String) metrics.get("language"));
                        project.setRepoName(repoName);
                        projectRepository.save(project);
                        updatedCount++;
                    }
                }
            }
        }
        log.info("GitHub project metrics synchronization complete. Total updated: {}", updatedCount);
        
        // Also refresh global stats
        redisUtil.delete(CacheConstants.GITHUB_STATS_CACHE_KEY);
    }

    private GitHubStatsResponse fetchGlobalStatsFromApi() {
        Map body = restClient.get()
                .uri(GITHUB_API_BASE + "/users/{username}", githubUsername)
                .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
                .headers(headers -> {
                    if (githubToken != null && !githubToken.isBlank()) {
                        headers.setBearerAuth(githubToken);
                    }
                })
                .retrieve()
                .body(Map.class);

        if (body != null) {
            return GitHubStatsResponse.builder()
                    .followers((Integer) body.get("followers"))
                    .publicRepos((Integer) body.get("public_repos"))
                    .htmlUrl((String) body.get("html_url"))
                    .avatarUrl((String) body.get("avatar_url"))
                    .totalStars(0)
                    .build();
        }
        return null;
    }

    public GitHubStatsResponse fallbackStats(Exception e) {
        log.error("GitHub stats fallback triggered due to: {}", e.getMessage());
        return GitHubStatsResponse.builder()
                .followers(0)
                .publicRepos(0)
                .totalStars(0)
                .htmlUrl("https://github.com/" + githubUsername)
                .build();
    }

    public Map<String, Object> fallbackMetrics(String repoName, Exception e) {
        log.error("GitHub metrics fallback triggered for {} due to: {}", repoName, e.getMessage());
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("stars", 0);
        fallback.put("forks", 0);
        fallback.put("language", "Unknown");
        return fallback;
    }

    private String extractRepoName(String githubUrl) {
        try {
            String[] parts = githubUrl.split("/");
            if (parts.length >= 5) {
                return parts[parts.length - 1].replace(".git", "");
            }
        } catch (Exception e) {
            log.warn("Could not extract repo name from URL: {}", githubUrl);
        }
        return null;
    }
}
