package space.nebula.nexus.service;

import space.nebula.nexus.payload.response.GitHubStatsResponse;
import space.nebula.nexus.payload.response.GitHubActivityResponse;

import java.time.YearMonth;
import java.util.Map;

public interface IGitHubService {

	/**
	 * Retrieves global stats for the configured GitHub user.
	 */
	GitHubStatsResponse retrieveGlobalStats();

	/**
	 * Retrieves public contribution activity for one calendar month.
	 *
	 * @param month
	 *            month to retrieve in UTC
	 */
	GitHubActivityResponse retrieveActivity(YearMonth month);

	/**
	 * Retrieves metrics for a specific repository.
	 * 
	 * @param repoName
	 *            repository name (e.g. "nexus")
	 */
	Map<String, Object> retrieveRepoMetrics(String repoName);

	/**
	 * Triggers a full synchronization of project metrics.
	 */
	void synchronizeProjectMetrics();
}
