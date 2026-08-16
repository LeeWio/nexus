package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Schema(description = "Public GitHub contribution activity for the configured profile")
public record GitHubActivityResponse(
		@Schema(description = "Calendar month in yyyy-MM format", example = "2026-08") String month,
		@Schema(description = "Localized month label", example = "August 2026") String periodLabel,
		@Schema(description = "GitHub display name or login", example = "LeeWio") String actor,
		@Schema(description = "GitHub login", example = "LeeWio") String login,
		@Schema(description = "GitHub profile URL") String profileUrl,
		@Schema(description = "Public repositories with commit contributions") List<RepositoryActivity> commitRepositories,
		@Schema(description = "Most recently merged public pull request") PullRequestActivity latestMergedPullRequest,
		@Schema(description = "Public repositories with opened issues") List<RepositoryActivity> issueRepositories,
		@Schema(description = "Public repositories with reviewed pull requests") List<RepositoryActivity> reviewRepositories,
		@Schema(description = "Total commits represented by commitRepositories") int totalCommits,
		@Schema(description = "Total issues represented by issueRepositories") int totalIssues,
		@Schema(description = "Opened issues still open in the returned contribution window") int openIssues,
		@Schema(description = "Opened issues now closed in the returned contribution window") int closedIssues,
		@Schema(description = "Total reviewed pull requests represented by reviewRepositories") int totalReviews,
		@Schema(description = "Most recent public pull request review time") Instant latestReviewAt,
		@Schema(description = "Time at which GitHub returned this snapshot") Instant fetchedAt,
		@Schema(description = "Whether a live or cached GitHub snapshot is available") boolean available)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	public GitHubActivityResponse {
		commitRepositories = List.copyOf(commitRepositories);
		issueRepositories = List.copyOf(issueRepositories);
		reviewRepositories = List.copyOf(reviewRepositories);
	}

	public static GitHubActivityResponse unavailable(String month, String periodLabel, String actor, String login,
			String profileUrl) {
		return new GitHubActivityResponse(month, periodLabel, actor, login, profileUrl, List.of(), null, List.of(),
				List.of(), 0, 0, 0, 0, 0, null, Instant.now(), false);
	}

	public record RepositoryActivity(@Schema(description = "Repository owner and name") String nameWithOwner,
			@Schema(description = "Public GitHub repository URL") String url,
			@Schema(description = "Contribution count for this activity type") int count) implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	public record PullRequestActivity(@Schema(description = "Pull request title") String title,
			@Schema(description = "Plain-text pull request description") String description,
			@Schema(description = "Repository owner and name") String repositoryNameWithOwner,
			@Schema(description = "Public pull request URL") String url,
			@Schema(description = "Pull request merge time") Instant mergedAt,
			@Schema(description = "Added lines") int additions, @Schema(description = "Deleted lines") int deletions,
			@Schema(description = "Pull request comment count") int commentCount) implements Serializable {
		private static final long serialVersionUID = 1L;
	}
}
