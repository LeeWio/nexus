package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;

import java.sql.ResultSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentIdempotencyServiceTest {

	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private ResultSet resultSet;

	private CommentIdempotencyService service;

	@BeforeEach
	void setUp() {
		service = new CommentIdempotencyService(jdbcTemplate);
	}

	@Test
	void hashSubmissionIsStableForSameSemanticRequest() {
		assertEquals(service.hashSubmission(1L, 2L, "hello"), service.hashSubmission(1L, 2L, "hello"));
		assertNotEquals(service.hashSubmission(1L, 2L, "hello"), service.hashSubmission(1L, 2L, "changed"));
	}

	@Test
	void beginCreatesNewRecordWhenKeyIsFresh()
	{
		when(jdbcTemplate.update(anyString(), eq(1L), eq("key"), eq("hash"))).thenReturn(1);

		Optional<ApiResponse<Void>> response = service.begin(1L, "key", "hash");

		assertTrue(response.isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	void beginReplaysCompletedResponseForSameHash() throws Exception
	{
		when(jdbcTemplate.update(anyString(), eq(1L), eq("key"), eq("hash"))).thenReturn(0);
		when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L), eq("key"))).thenAnswer(invocation ->
		{
			ResultSetExtractor<Optional<?>> extractor = invocation.getArgument(1);
			when(resultSet.next()).thenReturn(true);
			when(resultSet.getLong("comment_id")).thenReturn(10L);
			when(resultSet.wasNull()).thenReturn(false);
			when(resultSet.getObject("response_code", Integer.class)).thenReturn(200);
			when(resultSet.getString("request_hash")).thenReturn("hash");
			when(resultSet.getString("response_message")).thenReturn("stored response");
			return extractor.extractData(resultSet);
		});

		Optional<ApiResponse<Void>> response = service.begin(1L, "key", "hash");

		assertTrue(response.isPresent());
		assertEquals("stored response", response.get().message());
	}

	@Test
	@SuppressWarnings("unchecked")
	void beginRejectsSameKeyWithDifferentHash() throws Exception
	{
		when(jdbcTemplate.update(anyString(), eq(1L), eq("key"), eq("new-hash"))).thenReturn(0);
		when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L), eq("key"))).thenAnswer(invocation ->
		{
			ResultSetExtractor<Optional<?>> extractor = invocation.getArgument(1);
			when(resultSet.next()).thenReturn(true);
			when(resultSet.getLong("comment_id")).thenReturn(10L);
			when(resultSet.wasNull()).thenReturn(false);
			when(resultSet.getObject("response_code", Integer.class)).thenReturn(200);
			when(resultSet.getString("request_hash")).thenReturn("old-hash");
			when(resultSet.getString("response_message")).thenReturn("stored response");
			return extractor.extractData(resultSet);
		});

		assertThrows(BusinessException.class, () -> service.begin(1L, "key", "new-hash"));
	}
}
