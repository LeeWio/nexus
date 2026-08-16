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
import space.nebula.nexus.payload.github.GitHubGraphQlResponse;
import space.nebula.nexus.payload.github.GitHubGraphQlResponse.ContributionsByRepository;
import space.nebula.nexus.payload.github.GitHubGraphQlResponse.PullRequestContribution;
import space.nebula.nexus.payload.response.GitHubActivityResponse;
import space.nebula.nexus.payload.response.GitHubActivityResponse.PullRequestActivity;
import space.nebula.nexus.payload.response.GitHubActivityResponse.RepositoryActivity;
import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.service.IGitHubService;
import space.nebula.nexus.utils.RedisUtil;

import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements IGitHubService {

	private final RestClient restClient;
	private final RedisUtil redisUtil;
	private final ProjectRepository projectRepository;
	private final Executor outboundExecutor;

	@Value("${app.github.username}")
	private String githubUsername;

	@Value("${app.github.token:}")
	private String githubToken;

	private static final String GITHUB_API_BASE = "https://api.github.com";
	private static final String GITHUB_GRAPHQL_ENDPOINT = GITHUB_API_BASE + "/graphql";
	private static final int MAX_DESCRIPTION_LENGTH = 240;
	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
	private static final String ACTIVITY_QUERY = """
			query GitHubActivity($login: String!, $from: DateTime!, $to: DateTime!) {
			  user(login: $login) {
			    login
			    name
			    url
			    contributionsCollection(from: $from, to: $to) {
			      commitContributionsByRepository(maxRepositories: 4) {
			        repository { nameWithOwner url isPrivate }
			        contributions { totalCount }
			      }
			      issueContributionsByRepository(maxRepositories: 4) {
			        repository { nameWithOwner url isPrivate }
			        contributions { totalCount }
			      }
			      pullRequestReviewContributionsByRepository(maxRepositories: 4) {
			        repository { nameWithOwner url isPrivate }
			        contributions { totalCount }
			      }
			      pullRequestContributions(last: 10, orderBy: {direction: DESC}) {
			        nodes {
			          occurredAt
			          pullRequest {
			            title bodyText url mergedAt additions deletions
			            comments { totalCount }
			            repository { nameWithOwner url isPrivate }
			          }
			        }
			      }
			      issueContributions(first: 100, orderBy: {direction: DESC}) {
			        nodes {
			          occurredAt
			          issue { state repository { nameWithOwner url isPrivate } }
			        }
			      }
			      pullRequestReviewContributions(first: 100, orderBy: {direction: DESC}) {
			        nodes {
			          occurredAt
			          pullRequest { repository { nameWithOwner url isPrivate } }
			        }
			      }
			    }
			  }
			  rateLimit { cost remaining resetAt }
			}
			""";

	@Override
	@CircuitBreaker(name = "githubService", fallbackMethod = "fallbackActivity")
	public GitHubActivityResponse retrieveActivity(YearMonth month) {
		Objects.requireNonNull(month, "month must not be null");
		String cacheKey = CacheConstants.GITHUB_ACTIVITY_CACHE_PREFIX + month;
		return redisUtil.get(cacheKey, GitHubActivityResponse.class).orElseGet(() -> {
			GitHubActivityResponse activity = fetchActivityFromApi(month);
			redisUtil.set(cacheKey, activity, 1, TimeUnit.HOURS);
			return activity;
		});
	}

	@Override
	@CircuitBreaker(name = "githubService", fallbackMethod = "fallbackStats")
	@Retry(name = "githubService")
	public GitHubStatsResponse retrieveGlobalStats() {
		return redisUtil.get(CacheConstants.GITHUB_STATS_CACHE_KEY, GitHubStatsResponse.class).orElseGet(() -> {
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
				.accept(MediaType.parseMediaType("application/vnd.github.v3+json")).headers(headers -> {
					if (githubToken != null && !githubToken.isBlank()) {
						headers.setBearerAuth(githubToken);
					}
				}).retrieve().body(Map.class);

		if (body != null) {
			return Dict.create().set("stars", body.get("stargazers_count")).set("forks", body.get("forks_count"))
					.set("language", body.get("language"));
		}
		return null;
	}

	@Override
	public void synchronizeProjectMetrics() {
		log.info("Starting parallel synchronization of GitHub project metrics...");
		List<Project> projects = projectRepository.findAll();

		List<CompletableFuture<Project>> futures = projects.stream().map(
				project -> CompletableFuture.supplyAsync(() -> synchronizeProjectMetrics(project), outboundExecutor))
				.toList();

		List<Project> updatedProjects = futures.stream().map(CompletableFuture::join).filter(java.util.Objects::nonNull)
				.toList();

		if (!updatedProjects.isEmpty()) {
			projectRepository.saveAll(updatedProjects);
		}

		log.info("GitHub project metrics synchronization complete. Total updated: {}", updatedProjects.size());

		// Also refresh global stats
		redisUtil.delete(CacheConstants.GITHUB_STATS_CACHE_KEY);
	}

	private Project synchronizeProjectMetrics(Project project) {
		if (project.getGithubUrl() == null || project.getGithubUrl().isBlank()) {
			return null;
		}

		String repoName = extractRepoName(project.getGithubUrl());
		if (repoName == null) {
			return null;
		}

		try {
			Map<String, Object> metrics = retrieveRepoMetrics(repoName);
			if (metrics == null) {
				return null;
			}

			project.setStarsCount((Integer) metrics.get("stars"));
			project.setForksCount((Integer) metrics.get("forks"));
			project.setLanguage((String) metrics.get("language"));
			project.setRepoName(repoName);
			return project;
		} catch (RuntimeException exception) {
			log.warn("Skipping GitHub metric synchronization for {}: {}", repoName, exception.getMessage());
			return null;
		}
	}

	private GitHubStatsResponse fetchGlobalStatsFromApi() {
		Map body = restClient.get().uri(GITHUB_API_BASE + "/users/{username}", githubUsername)
				.accept(MediaType.parseMediaType("application/vnd.github.v3+json")).headers(headers -> {
					if (githubToken != null && !githubToken.isBlank()) {
						headers.setBearerAuth(githubToken);
					}
				}).retrieve().body(Map.class);

		if (body != null) {
			return GitHubStatsResponse.builder().followers((Integer) body.get("followers"))
					.publicRepos((Integer) body.get("public_repos")).htmlUrl((String) body.get("html_url"))
					.avatarUrl((String) body.get("avatar_url")).totalStars(0).build();
		}
		return null;
	}

	private GitHubActivityResponse fetchActivityFromApi(YearMonth month) {
		if (githubToken == null || githubToken.isBlank()) {
			throw new IllegalStateException("GitHub API token is not configured");
		}

		Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant to = month.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
		Map<String, Object> request = Map.of("query", ACTIVITY_QUERY, "variables",
				Map.of("login", githubUsername, "from", from.toString(), "to", to.toString()));

		GitHubGraphQlResponse response = restClient.post().uri(GITHUB_GRAPHQL_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.headers(headers -> headers.setBearerAuth(githubToken)).body(request).retrieve()
				.body(GitHubGraphQlResponse.class);

		if (response != null && response.errors() != null && !response.errors().isEmpty()) {
			throw new IllegalStateException("GitHub GraphQL request failed: " + response.errors().getFirst().message());
		}
		if (response == null || response.data() == null || response.data().user() == null
				|| response.data().user().contributionsCollection() == null) {
			throw new IllegalStateException("GitHub returned an incomplete activity response");
		}

		GitHubGraphQlResponse.User user = response.data().user();
		GitHubGraphQlResponse.ContributionsCollection contributions = user.contributionsCollection();
		List<RepositoryActivity> commits = mapRepositories(contributions.commitContributionsByRepository());
		List<RepositoryActivity> issues = mapRepositories(contributions.issueContributionsByRepository());
		List<RepositoryActivity> reviews = mapRepositories(contributions.pullRequestReviewContributionsByRepository());
		PullRequestActivity latestPullRequest = findLatestMergedPullRequest(contributions.pullRequestContributions());

		long openIssues = safeIssueContributions(contributions).stream().filter(this::isPublicIssue)
				.filter(item -> "OPEN".equals(item.issue().state())).count();
		long closedIssues = safeIssueContributions(contributions).stream().filter(this::isPublicIssue)
				.filter(item -> "CLOSED".equals(item.issue().state())).count();
		Instant latestReviewAt = safeReviewContributions(contributions).stream().filter(this::isPublicReview)
				.map(GitHubGraphQlResponse.ReviewContribution::occurredAt).filter(Objects::nonNull)
				.max(Comparator.naturalOrder()).orElse(null);

		if (response.data().rateLimit() != null) {
			log.debug("GitHub activity query cost={}, remaining={}, resetAt={}", response.data().rateLimit().cost(),
					response.data().rateLimit().remaining(), response.data().rateLimit().resetAt());
		}

		String login = user.login() == null || user.login().isBlank() ? githubUsername : user.login();
		String actor = user.name() == null || user.name().isBlank() ? login : user.name();
		String profileUrl = isPublicGitHubUrl(user.url()) ? user.url() : "https://github.com/" + login;
		return new GitHubActivityResponse(month.toString(), MONTH_LABEL_FORMATTER.format(month), actor, login,
				profileUrl, commits, latestPullRequest, issues, reviews, sumCounts(commits), sumCounts(issues),
				Math.toIntExact(openIssues), Math.toIntExact(closedIssues), sumCounts(reviews), latestReviewAt,
				Instant.now(), true);
	}

	private List<RepositoryActivity> mapRepositories(List<ContributionsByRepository> repositories) {
		if (repositories == null) {
			return List.of();
		}
		return repositories.stream().filter(Objects::nonNull).filter(item -> item.repository() != null)
				.filter(item -> !item.repository().isPrivate())
				.filter(item -> isPublicGitHubUrl(item.repository().url()))
				.filter(item -> item.contributions() != null && item.contributions().totalCount() > 0)
				.map(item -> new RepositoryActivity(item.repository().nameWithOwner(), item.repository().url(),
						item.contributions().totalCount()))
				.toList();
	}

	private PullRequestActivity findLatestMergedPullRequest(
			GitHubGraphQlResponse.PullRequestContributionConnection contributions) {
		if (contributions == null || contributions.nodes() == null) {
			return null;
		}
		return contributions.nodes().stream().filter(Objects::nonNull).filter(this::isPublicMergedPullRequest)
				.max(Comparator.comparing(item -> item.pullRequest().mergedAt()))
				.map(PullRequestContribution::pullRequest)
				.map(pullRequest -> new PullRequestActivity(pullRequest.title(), summarize(pullRequest.bodyText()),
						pullRequest.repository().nameWithOwner(), pullRequest.url(), pullRequest.mergedAt(),
						pullRequest.additions() == null ? 0 : pullRequest.additions(),
						pullRequest.deletions() == null ? 0 : pullRequest.deletions(),
						pullRequest.comments() == null ? 0 : pullRequest.comments().totalCount()))
				.orElse(null);
	}

	private boolean isPublicMergedPullRequest(PullRequestContribution contribution) {
		return contribution.pullRequest() != null && contribution.pullRequest().mergedAt() != null
				&& contribution.pullRequest().repository() != null
				&& !contribution.pullRequest().repository().isPrivate()
				&& isPublicGitHubUrl(contribution.pullRequest().url());
	}

	private List<GitHubGraphQlResponse.IssueContribution> safeIssueContributions(
			GitHubGraphQlResponse.ContributionsCollection contributions) {
		return contributions.issueContributions() == null || contributions.issueContributions().nodes() == null
				? List.of()
				: contributions.issueContributions().nodes();
	}

	private List<GitHubGraphQlResponse.ReviewContribution> safeReviewContributions(
			GitHubGraphQlResponse.ContributionsCollection contributions) {
		return contributions.pullRequestReviewContributions() == null
				|| contributions.pullRequestReviewContributions().nodes() == null
						? List.of()
						: contributions.pullRequestReviewContributions().nodes();
	}

	private boolean isPublicIssue(GitHubGraphQlResponse.IssueContribution contribution) {
		return contribution != null && contribution.issue() != null && contribution.issue().repository() != null
				&& !contribution.issue().repository().isPrivate();
	}

	private boolean isPublicReview(GitHubGraphQlResponse.ReviewContribution contribution) {
		return contribution != null && contribution.pullRequest() != null
				&& contribution.pullRequest().repository() != null
				&& !contribution.pullRequest().repository().isPrivate();
	}

	private int sumCounts(List<RepositoryActivity> repositories) {
		return repositories.stream().mapToInt(RepositoryActivity::count).sum();
	}

	private String summarize(String value) {
		if (value == null || value.isBlank()) {
			return "Merged pull request";
		}
		String normalized = value.replaceAll("\\s+", " ").trim();
		return normalized.length() <= MAX_DESCRIPTION_LENGTH
				? normalized
				: normalized.substring(0, MAX_DESCRIPTION_LENGTH - 1) + "…";
	}

	private boolean isPublicGitHubUrl(String value) {
		return value != null && value.startsWith("https://github.com/");
	}

	public GitHubStatsResponse fallbackStats(Exception e) {
		log.error("GitHub stats fallback triggered due to: {}", e.getMessage());
		return GitHubStatsResponse.builder().followers(0).publicRepos(0).totalStars(0)
				.htmlUrl("https://github.com/" + githubUsername).build();
	}

	public Map<String, Object> fallbackMetrics(String repoName, Exception e) {
		log.error("GitHub metrics fallback triggered for {} due to: {}", repoName, e.getMessage());
		return Dict.create().set("stars", 0).set("forks", 0).set("language", "Unknown");
	}

	public GitHubActivityResponse fallbackActivity(YearMonth month, Exception exception) {
		log.warn("GitHub activity fallback triggered for {} due to {}: {}", month, exception.getClass().getSimpleName(),
				exception.getMessage());
		String login = githubUsername == null ? "" : githubUsername;
		return GitHubActivityResponse.unavailable(month.toString(), MONTH_LABEL_FORMATTER.format(month), login, login,
				"https://github.com/" + login);
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
