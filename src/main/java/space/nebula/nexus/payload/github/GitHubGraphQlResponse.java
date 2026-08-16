package space.nebula.nexus.payload.github;

import java.time.Instant;
import java.util.List;

public record GitHubGraphQlResponse(Data data, List<GraphQlError> errors) {

	public record Data(User user, RateLimit rateLimit) {
	}

	public record User(String login, String name, String url, ContributionsCollection contributionsCollection) {
	}

	public record ContributionsCollection(List<ContributionsByRepository> commitContributionsByRepository,
			List<ContributionsByRepository> issueContributionsByRepository,
			List<ContributionsByRepository> pullRequestReviewContributionsByRepository,
			PullRequestContributionConnection pullRequestContributions, IssueContributionConnection issueContributions,
			ReviewContributionConnection pullRequestReviewContributions) {
	}

	public record ContributionsByRepository(Repository repository, ContributionCount contributions) {
	}

	public record Repository(String nameWithOwner, String url, boolean isPrivate) {
	}

	public record ContributionCount(int totalCount) {
	}

	public record PullRequestContributionConnection(List<PullRequestContribution> nodes) {
	}

	public record PullRequestContribution(Instant occurredAt, PullRequest pullRequest) {
	}

	public record IssueContributionConnection(List<IssueContribution> nodes) {
	}

	public record IssueContribution(Instant occurredAt, Issue issue) {
	}

	public record Issue(String state, Repository repository) {
	}

	public record ReviewContributionConnection(List<ReviewContribution> nodes) {
	}

	public record ReviewContribution(Instant occurredAt, PullRequest pullRequest) {
	}

	public record PullRequest(String title, String bodyText, String url, Instant mergedAt, Integer additions,
			Integer deletions, ContributionCount comments, Repository repository) {
	}

	public record RateLimit(int cost, int remaining, Instant resetAt) {
	}

	public record GraphQlError(String message) {
	}
}
