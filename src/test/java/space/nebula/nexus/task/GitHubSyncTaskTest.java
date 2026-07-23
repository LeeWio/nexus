package space.nebula.nexus.task;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.service.IGitHubService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GitHubSyncTaskTest {
	@Test
	void syncGitHubMetricsDelegatesSynchronizationToService() {
		IGitHubService githubService = mock(IGitHubService.class);
		GitHubSyncTask task = new GitHubSyncTask(githubService);

		task.syncGitHubMetrics();

		verify(githubService).synchronizeProjectMetrics();
	}
}
