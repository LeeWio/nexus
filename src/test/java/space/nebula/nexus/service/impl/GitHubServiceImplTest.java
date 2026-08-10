package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitHubServiceImplTest {

	@Test
	void synchronizeProjectMetricsContinuesAfterAnIndividualRepositoryFails() {
		ProjectRepository projectRepository = mock(ProjectRepository.class);
		RedisUtil redisUtil = mock(RedisUtil.class);
		Executor directExecutor = Runnable::run;
		GitHubServiceImpl service = Mockito.spy(
				new GitHubServiceImpl(mock(org.springframework.web.client.RestClient.class), redisUtil, projectRepository,
						directExecutor));

		Project unavailableProject = project("https://github.com/nebula/unavailable");
		Project updatedProject = project("https://github.com/nebula/available");
		when(projectRepository.findAll()).thenReturn(List.of(unavailableProject, updatedProject));
		doThrow(new IllegalStateException("GitHub unavailable")).when(service).retrieveRepoMetrics("unavailable");
		doReturn(Map.of("stars", 42, "forks", 7, "language", "Java")).when(service).retrieveRepoMetrics("available");

		service.synchronizeProjectMetrics();

		verify(projectRepository).saveAll(List.of(updatedProject));
		assertEquals(42, updatedProject.getStarsCount());
		assertEquals(7, updatedProject.getForksCount());
		verify(redisUtil).delete(CacheConstants.GITHUB_STATS_CACHE_KEY);
	}

	private Project project(String githubUrl) {
		Project project = new Project();
		project.setGithubUrl(githubUrl);
		return project;
	}
}
