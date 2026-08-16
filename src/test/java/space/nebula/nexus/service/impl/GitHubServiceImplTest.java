package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.payload.response.GitHubActivityResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubServiceImplTest {
	@Test
	void retrieveActivityMapsOnlyPublicContributionsAndCachesTheSnapshot() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		RedisUtil redisUtil = mock(RedisUtil.class);
		GitHubServiceImpl service = new GitHubServiceImpl(restClientBuilder.build(), redisUtil,
				mock(ProjectRepository.class), Runnable::run);
		ReflectionTestUtils.setField(service, "githubUsername", "LeeWio");
		ReflectionTestUtils.setField(service, "githubToken", "test-token");
		YearMonth month = YearMonth.of(2026, 8);
		String cacheKey = CacheConstants.GITHUB_ACTIVITY_CACHE_PREFIX + month;
		when(redisUtil.get(cacheKey, GitHubActivityResponse.class)).thenReturn(Optional.empty());

		server.expect(requestTo("https://api.github.com/graphql"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("GitHubActivity")))
				.andRespond(withSuccess(GITHUB_ACTIVITY_RESPONSE, MediaType.APPLICATION_JSON));

		GitHubActivityResponse activity = service.retrieveActivity(month);

		assertTrue(activity.available());
		assertEquals("August 2026", activity.periodLabel());
		assertEquals("Wei Li", activity.actor());
		assertEquals(12, activity.totalCommits());
		assertEquals(3, activity.totalIssues());
		assertEquals(1, activity.openIssues());
		assertEquals(1, activity.closedIssues());
		assertEquals(4, activity.totalReviews());
		assertEquals(1, activity.commitRepositories().size());
		assertFalse(activity.commitRepositories().stream()
				.anyMatch(repository -> repository.nameWithOwner().contains("private")));
		assertNotNull(activity.latestMergedPullRequest());
		assertEquals("LeeWio/odyssey", activity.latestMergedPullRequest().repositoryNameWithOwner());
		assertEquals(18, activity.latestMergedPullRequest().additions());
		assertEquals("2026-08-13T08:00:00Z", activity.latestReviewAt().toString());
		verify(redisUtil).set(cacheKey, activity, 1, TimeUnit.HOURS);
		server.verify();
	}

	@Test
	void synchronizeProjectMetricsContinuesAfterAnIndividualRepositoryFails() {
		ProjectRepository projectRepository = mock(ProjectRepository.class);
		RedisUtil redisUtil = mock(RedisUtil.class);
		Executor directExecutor = Runnable::run;
		GitHubServiceImpl service = Mockito.spy(new GitHubServiceImpl(
				mock(org.springframework.web.client.RestClient.class), redisUtil, projectRepository, directExecutor));

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

	private static final String GITHUB_ACTIVITY_RESPONSE = """
			{
			  "data": {
			    "user": {
			      "login": "LeeWio",
			      "name": "Wei Li",
			      "url": "https://github.com/LeeWio",
			      "contributionsCollection": {
			        "commitContributionsByRepository": [
			          {"repository":{"nameWithOwner":"LeeWio/odyssey","url":"https://github.com/LeeWio/odyssey","isPrivate":false},"contributions":{"totalCount":12}},
			          {"repository":{"nameWithOwner":"LeeWio/private","url":"https://github.com/LeeWio/private","isPrivate":true},"contributions":{"totalCount":99}}
			        ],
			        "issueContributionsByRepository": [
			          {"repository":{"nameWithOwner":"LeeWio/odyssey","url":"https://github.com/LeeWio/odyssey","isPrivate":false},"contributions":{"totalCount":3}}
			        ],
			        "pullRequestReviewContributionsByRepository": [
			          {"repository":{"nameWithOwner":"LeeWio/nexus","url":"https://github.com/LeeWio/nexus","isPrivate":false},"contributions":{"totalCount":4}}
			        ],
			        "pullRequestContributions": {"nodes":[
			          {"occurredAt":"2026-08-12T08:00:00Z","pullRequest":{"title":"Ship activity API","bodyText":"Adds the real GitHub activity feed.","url":"https://github.com/LeeWio/odyssey/pull/42","mergedAt":"2026-08-12T09:00:00Z","additions":18,"deletions":5,"comments":{"totalCount":3},"repository":{"nameWithOwner":"LeeWio/odyssey","url":"https://github.com/LeeWio/odyssey","isPrivate":false}}},
			          {"occurredAt":"2026-08-14T08:00:00Z","pullRequest":{"title":"Private change","bodyText":"hidden","url":"https://github.com/LeeWio/private/pull/1","mergedAt":"2026-08-14T09:00:00Z","additions":1,"deletions":1,"comments":{"totalCount":0},"repository":{"nameWithOwner":"LeeWio/private","url":"https://github.com/LeeWio/private","isPrivate":true}}}
			        ]},
			        "issueContributions": {"nodes":[
			          {"occurredAt":"2026-08-04T08:00:00Z","issue":{"state":"OPEN","repository":{"nameWithOwner":"LeeWio/odyssey","url":"https://github.com/LeeWio/odyssey","isPrivate":false}}},
			          {"occurredAt":"2026-08-05T08:00:00Z","issue":{"state":"CLOSED","repository":{"nameWithOwner":"LeeWio/odyssey","url":"https://github.com/LeeWio/odyssey","isPrivate":false}}},
			          {"occurredAt":"2026-08-06T08:00:00Z","issue":{"state":"OPEN","repository":{"nameWithOwner":"LeeWio/private","url":"https://github.com/LeeWio/private","isPrivate":true}}}
			        ]},
			        "pullRequestReviewContributions": {"nodes":[
			          {"occurredAt":"2026-08-13T08:00:00Z","pullRequest":{"repository":{"nameWithOwner":"LeeWio/nexus","url":"https://github.com/LeeWio/nexus","isPrivate":false}}}
			        ]}
			      }
			    },
			    "rateLimit": {"cost":1,"remaining":4999,"resetAt":"2026-08-16T12:00:00Z"}
			  }
			}
			""";
}
