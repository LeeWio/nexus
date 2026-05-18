package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.service.IGitHubService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements IGitHubService {

    private final RestTemplate restTemplate;
    private final RedisUtil redisUtil;
    private final ProjectRepository projectRepository;

    @Value("${app.github.username}")
    private String githubUsername;

    @Value("${app.github.token:}")
    private String githubToken;

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String STATS_CACHE_KEY = "nexus:github:global_stats";

    @Override
    public GitHubStatsResponse retrieveGlobalStats() {
        return redisUtil.get(STATS_CACHE_KEY, GitHubStatsResponse.class)
                .orElseGet(() -> {
                    log.info("GitHub global stats cache miss, fetching from API...");
                    GitHubStatsResponse stats = fetchGlobalStatsFromApi();
                    if (stats != null) {
                        redisUtil.set(STATS_CACHE_KEY, stats, 6, TimeUnit.HOURS);
                    }
                    return stats;
                });
    }

    @Override
    public Map<String, Object> retrieveRepoMetrics(String repoName) {
        String url = String.format("%s/repos/%s/%s", GITHUB_API_BASE, githubUsername, repoName);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, createAuthEntity(), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("stars", body.get("stargazers_count"));
                metrics.put("forks", body.get("forks_count"));
                metrics.put("language", body.get("language"));
                return metrics;
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub metrics for repo: {}", repoName, e);
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
        redisUtil.delete(STATS_CACHE_KEY);
    }

    private GitHubStatsResponse fetchGlobalStatsFromApi() {
        String url = String.format("%s/users/%s", GITHUB_API_BASE, githubUsername);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, createAuthEntity(), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                return GitHubStatsResponse.builder()
                        .followers((Integer) body.get("followers"))
                        .publicRepos((Integer) body.get("public_repos"))
                        .htmlUrl((String) body.get("html_url"))
                        .avatarUrl((String) body.get("avatar_url"))
                        .totalStars(0) // GitHub API doesn't provide total stars easily, we'd need to iterate
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub global stats for user: {}", githubUsername, e);
        }
        return null;
    }

    private HttpEntity<String> createAuthEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        if (githubToken != null && !githubToken.isBlank()) {
            headers.set("Authorization", "token " + githubToken);
        }
        return new HttpEntity<>(headers);
    }

    private String extractRepoName(String githubUrl) {
        try {
            // Format: https://github.com/owner/repo
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
