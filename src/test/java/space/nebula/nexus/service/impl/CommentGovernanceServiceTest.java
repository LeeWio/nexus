package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import space.nebula.nexus.config.CommentModerationProperties;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.CommentGovernanceOverviewResponse;
import space.nebula.nexus.payload.response.CommentModerationLogResponse;
import space.nebula.nexus.payload.response.CommentRiskResponse;
import space.nebula.nexus.payload.response.CommentReportResponse;
import space.nebula.nexus.repository.CommentModerationLogRepository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentGovernanceServiceTest
{

	@Mock
	private CommentModerationLogRepository moderationLogRepository;
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private ResultSet resultSet;

	private CommentGovernanceService governanceService;
	private CommentModerationProperties moderationProperties;

	@BeforeEach
	void setUp()
	{
		moderationProperties = new CommentModerationProperties();
		governanceService = new CommentGovernanceService(moderationLogRepository, jdbcTemplate, moderationProperties);
	}

	@Test
	@SuppressWarnings("unchecked")
	void retrieveCommentGovernanceOverviewReturnsOperationalCounters() throws Exception
	{
		ResultSet commentStatusRow = mock(ResultSet.class);
		ResultSet reportStatusRow = mock(ResultSet.class);
		ResultSet actionRow = mock(ResultSet.class);
		when(commentStatusRow.getString("status")).thenReturn("PENDING");
		when(commentStatusRow.getLong("item_count")).thenReturn(4L);
		when(reportStatusRow.getString("status")).thenReturn("OPEN");
		when(reportStatusRow.getLong("item_count")).thenReturn(2L);
		when(actionRow.getString("action")).thenReturn("AUTO_FLAGGED");
		when(actionRow.getLong("item_count")).thenReturn(1L);
		when(jdbcTemplate.queryForObject(contains("blog_comment_report WHERE created_at"), eq(Long.class),
				any(Object[].class))).thenReturn(3L);
		when(jdbcTemplate.queryForObject(contains("blog_comment_moderation_log WHERE is_deleted = FALSE AND action"),
				eq(Long.class), any(Object[].class))).thenReturn(1L);
		when(jdbcTemplate.queryForObject(contains("SELECT MIN(created_at)"), eq(Timestamp.class),
				any(Object[].class))).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 7, 23, 8, 0)));
		doAnswer(invocation ->
		{
			RowCallbackHandler handler = invocation.getArgument(1);
			handler.processRow(commentStatusRow);
			return null;
		}).when(jdbcTemplate).query(eq("SELECT status, COUNT(*) AS item_count FROM blog_comment WHERE is_deleted = FALSE GROUP BY status"),
				any(RowCallbackHandler.class));
		doAnswer(invocation ->
		{
			RowCallbackHandler handler = invocation.getArgument(1);
			handler.processRow(reportStatusRow);
			return null;
		}).when(jdbcTemplate).query(eq("SELECT status, COUNT(*) AS item_count FROM blog_comment_report GROUP BY status"),
				any(RowCallbackHandler.class));
		when(jdbcTemplate.query(contains("GROUP BY action"), any(RowMapper.class), any(Object[].class)))
				.thenAnswer(invocation ->
				{
					RowMapper<CommentGovernanceOverviewResponse.ModerationActionCount> mapper =
							invocation.getArgument(1);
					return List.of(mapper.mapRow(actionRow, 0));
				});

		var response = governanceService.retrieveCommentGovernanceOverview();

		assertEquals(200, response.code());
		assertEquals(4L, response.data().totalComments());
		assertEquals(4L, response.data().pendingComments());
		assertEquals(2L, response.data().openReports());
		assertEquals(3L, response.data().reportsLast24Hours());
		assertEquals(1L, response.data().autoFlaggedLast24Hours());
		assertEquals(LocalDateTime.of(2026, 7, 23, 8, 0), response.data().oldestPendingAt());
		assertEquals(4, response.data().commentsByStatus().size());
		assertEquals(3, response.data().reportsByStatus().size());
		assertEquals(CommentModerationAction.AUTO_FLAGGED,
				response.data().moderationActionsLast7Days().getFirst().action());
	}

	@Test
	@SuppressWarnings("unchecked")
	void retrieveCommentReportsReturnsPagedReportQueue() throws Exception
	{
		var pageable = PageRequest.of(1, 10);
		when(jdbcTemplate.queryForObject(contains("FROM blog_comment_report"), eq(Long.class), any(Object[].class)))
				.thenReturn(21L);
		ArgumentCaptor<RowMapper<CommentReportResponse>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
		when(jdbcTemplate.query(contains("ORDER BY report.created_at DESC"), mapperCaptor.capture(),
				any(Object[].class))).thenAnswer(invocation ->
		{
			RowMapper<CommentReportResponse> mapper = invocation.getArgument(1);
			stubReportRow();
			return List.of(mapper.mapRow(resultSet, 0));
		});

		var response = governanceService.retrieveCommentReports(CommentReportStatus.OPEN, 42L, "reporter", pageable);

		assertEquals(200, response.code());
		assertEquals(21L, response.data().getTotal());
		assertEquals(2, response.data().getPage());
		assertEquals(3, response.data().getTotalPages());
		CommentReportResponse report = response.data().getList().getFirst();
		assertEquals(42L, report.commentId());
		assertEquals(CommentReportStatus.OPEN, report.status());
		assertEquals(CommentStatus.APPROVED, report.commentStatus());
		assertEquals("reporter", report.reporterUsername());
		verify(jdbcTemplate).query(contains("reporter.username = ?"), any(RowMapper.class),
				aryEq(new Object[] { CommentReportStatus.OPEN.name(), 42L, "reporter", 10, 10L }));
	}

	@Test
	@SuppressWarnings("unchecked")
	void retrieveHighRiskCommentsUsesConfiguredThresholdWhenMissing() throws Exception
	{
		moderationProperties.setHighRiskReportThreshold(4L);
		var pageable = PageRequest.of(0, 10);
		when(jdbcTemplate.queryForObject(contains("high_risk"), eq(Long.class), any(Object[].class))).thenReturn(1L);
		when(jdbcTemplate.query(contains("ORDER BY open_reports DESC"), any(RowMapper.class), any(Object[].class)))
				.thenAnswer(invocation ->
				{
					RowMapper<CommentRiskResponse> mapper = invocation.getArgument(1);
					stubRiskRow();
					return List.of(mapper.mapRow(resultSet, 0));
				});

		var response = governanceService.retrieveHighRiskComments(null, pageable);

		assertEquals(200, response.code());
		assertEquals(1L, response.data().getTotal());
		assertEquals(42L, response.data().getList().getFirst().id());
		assertEquals(5L, response.data().getList().getFirst().openReports());
		assertEquals(70L, response.data().getList().getFirst().riskScore());
		verify(jdbcTemplate).queryForObject(contains("high_risk"), eq(Long.class),
				aryEq(new Object[] { CommentReportStatus.OPEN.name(), 4L }));
		verify(jdbcTemplate).query(contains("ORDER BY open_reports DESC"), any(RowMapper.class),
				aryEq(new Object[] { CommentReportStatus.OPEN.name(), 4L, 10, 0L }));
	}

	@Test
	@SuppressWarnings("unchecked")
	void retrieveCommentModerationLogsReturnsPagedAuditTrail() throws Exception
	{
		var pageable = PageRequest.of(0, 5);
		when(jdbcTemplate.queryForObject(contains("FROM blog_comment_moderation_log"), eq(Long.class),
				any(Object[].class))).thenReturn(1L);
		when(jdbcTemplate.query(contains("ORDER BY log.created_at DESC"), any(RowMapper.class), any(Object[].class)))
				.thenAnswer(invocation ->
				{
					RowMapper<CommentModerationLogResponse> mapper = invocation.getArgument(1);
					stubModerationLogRow();
					return List.of(mapper.mapRow(resultSet, 0));
				});

		var response = governanceService.retrieveCommentModerationLogs(42L, CommentModerationAction.STATUS_CHANGED,
				pageable);

		assertEquals(200, response.code());
		assertEquals(1L, response.data().getTotal());
		CommentModerationLogResponse log = response.data().getList().getFirst();
		assertEquals(7L, log.id());
		assertEquals(42L, log.commentId());
		assertEquals(CommentModerationAction.STATUS_CHANGED, log.action());
		assertEquals(CommentStatus.PENDING, log.previousStatus());
		assertEquals(CommentStatus.APPROVED, log.newStatus());
		verify(jdbcTemplate).query(contains("log.action = ?"), any(RowMapper.class),
				aryEq(new Object[] { 42L, CommentModerationAction.STATUS_CHANGED.name(), 5, 0L }));
	}

	private void stubReportRow() throws Exception
	{
		when(resultSet.getLong("comment_id")).thenReturn(42L);
		when(resultSet.getLong("reporter_id")).thenReturn(9L);
		when(resultSet.getString("reporter_username")).thenReturn("reporter");
		when(resultSet.getString("reporter_nickname")).thenReturn("Reporter");
		when(resultSet.getString("reason")).thenReturn("spam");
		when(resultSet.getString("description")).thenReturn("details");
		when(resultSet.getString("status")).thenReturn("OPEN");
		when(resultSet.getString("handled_by")).thenReturn(null);
		when(resultSet.getTimestamp("handled_at")).thenReturn(null);
		when(resultSet.getString("resolution_note")).thenReturn(null);
		when(resultSet.getLong("post_id")).thenReturn(5L);
		when(resultSet.wasNull()).thenReturn(false, false);
		when(resultSet.getString("post_title")).thenReturn("Post");
		when(resultSet.getLong("parent_id")).thenReturn(1L);
		when(resultSet.getString("comment_status")).thenReturn("APPROVED");
		when(resultSet.getString("comment_content")).thenReturn("content");
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 7, 23, 9, 0)));
	}

	private void stubModerationLogRow() throws Exception
	{
		when(resultSet.getLong("id")).thenReturn(7L);
		when(resultSet.getLong("comment_id")).thenReturn(42L);
		when(resultSet.getLong("post_id")).thenReturn(5L);
		when(resultSet.wasNull()).thenReturn(false, false);
		when(resultSet.getString("post_title")).thenReturn("Post");
		when(resultSet.getLong("parent_id")).thenReturn(1L);
		when(resultSet.getString("comment_content")).thenReturn("content");
		when(resultSet.getString("action")).thenReturn("STATUS_CHANGED");
		when(resultSet.getString("previous_status")).thenReturn("PENDING");
		when(resultSet.getString("new_status")).thenReturn("APPROVED");
		when(resultSet.getString("moderator_username")).thenReturn("admin");
		when(resultSet.getString("reason")).thenReturn("MANUAL_MODERATION");
		when(resultSet.getString("batch_id")).thenReturn(null);
		when(resultSet.getString("note")).thenReturn(null);
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 7, 23, 9, 0)));
	}

	private void stubRiskRow() throws Exception
	{
		when(resultSet.getLong("id")).thenReturn(42L);
		when(resultSet.getLong("parent_id")).thenReturn(1L);
		when(resultSet.getLong("post_id")).thenReturn(5L);
		when(resultSet.wasNull()).thenReturn(false, false);
		when(resultSet.getString("post_title")).thenReturn("Post");
		when(resultSet.getString("content")).thenReturn("content");
		when(resultSet.getString("username")).thenReturn("author");
		when(resultSet.getString("nickname")).thenReturn("Author");
		when(resultSet.getString("avatar")).thenReturn("avatar.png");
		when(resultSet.getString("status")).thenReturn("PENDING");
		when(resultSet.getLong("reports_count")).thenReturn(10L);
		when(resultSet.getLong("open_reports")).thenReturn(5L);
		when(resultSet.getLong("likes_count")).thenReturn(2L);
		when(resultSet.getLong("risk_score")).thenReturn(70L);
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 7, 23, 9, 0)));
		when(resultSet.getTimestamp("edited_at")).thenReturn(null);
	}
}
