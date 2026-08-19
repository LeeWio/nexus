package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.repository.LinkCheckLogRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkHealthLogPersistenceServiceTest {

	@Mock
	private LinkCheckLogRepository linkCheckLogRepository;

	@InjectMocks
	private LinkHealthLogPersistenceService persistenceService;

	@Test
	void saveBatchLoadsExistingLogsOnceAndPersistsUpdatesTogether() {
		LinkCheckLog existingLog = new LinkCheckLog();
		existingLog.setId(7L);
		existingLog.setUrl("https://example.com/existing");
		existingLog.setSourceType("POST");
		existingLog.setSourceId(11L);
		when(linkCheckLogRepository.findBySourceTypeAndSourceIdIn("POST", Set.of(11L, 12L)))
				.thenReturn(List.of(existingLog));

		persistenceService.saveBatch(List.of(
				new LinkHealthLogPersistenceService.LinkCheckLogUpdate("https://example.com/existing", "POST", 11L,
						"Existing post", 200, false, null),
				new LinkHealthLogPersistenceService.LinkCheckLogUpdate("https://example.com/new", "POST", 12L,
						"New post", 404, true, "Not found")));

		verify(linkCheckLogRepository).findBySourceTypeAndSourceIdIn("POST", Set.of(11L, 12L));
		verify(linkCheckLogRepository, never()).findByUrlAndSourceTypeAndSourceId(eq("https://example.com/existing"),
				eq("POST"), eq(11L));

		@SuppressWarnings({"unchecked", "rawtypes"})
		ArgumentCaptor<Iterable<LinkCheckLog>> logsCaptor = ArgumentCaptor.forClass((Class) Iterable.class);
		verify(linkCheckLogRepository).saveAll(logsCaptor.capture());
		List<LinkCheckLog> savedLogs = StreamSupport.stream(logsCaptor.getValue().spliterator(), false).toList();

		assertEquals(2, savedLogs.size());
		assertSame(existingLog, savedLogs.get(0));
		assertEquals(200, existingLog.getStatusCode());
		assertEquals("https://example.com/new", savedLogs.get(1).getUrl());
		assertTrue(savedLogs.get(1).getIsBroken());
	}

	@Test
	void saveBatchSkipsDatabaseWorkForEmptyResults() {
		persistenceService.saveBatch(List.of());

		verifyNoInteractions(linkCheckLogRepository);
	}
}
