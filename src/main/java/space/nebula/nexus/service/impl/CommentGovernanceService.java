package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.config.CommentModerationProperties;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.CommentModerationLog;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.CommentGovernanceOverviewResponse;
import space.nebula.nexus.payload.response.CommentModerationLogResponse;
import space.nebula.nexus.payload.response.CommentRiskResponse;
import space.nebula.nexus.payload.response.CommentReportResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.CommentModerationLogRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentGovernanceService
{

	private final CommentModerationLogRepository moderationLogRepository;
	private final JdbcTemplate jdbcTemplate;
	private final CommentModerationProperties moderationProperties;

	public void recordModeration(Comment comment, CommentStatus previousStatus, CommentStatus newStatus,
			CommentModerationAction action, String reason, String note, String batchId)
	{
		CommentModerationLog log = new CommentModerationLog();
		log.setComment(comment);
		log.setModeratorUsername(SecurityUtil.getCurrentUsername());
		log.setPreviousStatus(previousStatus);
		log.setNewStatus(newStatus);
		log.setAction(action);
		log.setReason(reason);
		log.setNote(note);
		log.setBatchId(batchId);
		moderationLogRepository.save(log);
	}

	public long countOpenReports(Long commentId)
	{
		Long count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM blog_comment_report WHERE comment_id = ? AND status = ?",
				Long.class, commentId, CommentReportStatus.OPEN.name());
		return count == null ? 0L : count;
	}

	public void resolveOpenReports(Long commentId, CommentReportStatus status, String resolutionNote)
	{
		jdbcTemplate.update(
				"UPDATE blog_comment_report SET status = ?, resolution_note = ?, handled_by = ?, handled_at = CURRENT_TIMESTAMP WHERE comment_id = ? AND status = ?",
				status.name(), resolutionNote, SecurityUtil.getCurrentUsername(), commentId, CommentReportStatus.OPEN.name());
	}

	public ApiResponse<CommentGovernanceOverviewResponse> retrieveCommentGovernanceOverview()
	{
		LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
		LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
		Map<CommentStatus, Long> commentCounts = retrieveCommentStatusCounts();
		Map<CommentReportStatus, Long> reportCounts = retrieveReportStatusCounts();
		List<CommentGovernanceOverviewResponse.ModerationActionCount> moderationActionsLast7Days =
				retrieveModerationActionCounts(last7Days);

		Long reportsLast24Hours = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM blog_comment_report WHERE created_at >= ?", Long.class, last24Hours);
		Long autoFlaggedLast24Hours = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM blog_comment_moderation_log WHERE is_deleted = FALSE AND action = ? AND created_at >= ?",
				Long.class, CommentModerationAction.AUTO_FLAGGED.name(), last24Hours);
		LocalDateTime oldestPendingAt = queryLocalDateTime(
				"SELECT MIN(created_at) FROM blog_comment WHERE is_deleted = FALSE AND status = ?",
				CommentStatus.PENDING.name());

		CommentGovernanceOverviewResponse overview = CommentGovernanceOverviewResponse.builder()
				.totalComments(sum(commentCounts.values()))
				.pendingComments(commentCounts.getOrDefault(CommentStatus.PENDING, 0L))
				.approvedComments(commentCounts.getOrDefault(CommentStatus.APPROVED, 0L))
				.rejectedComments(commentCounts.getOrDefault(CommentStatus.REJECTED, 0L))
				.spamComments(commentCounts.getOrDefault(CommentStatus.SPAM, 0L))
				.openReports(reportCounts.getOrDefault(CommentReportStatus.OPEN, 0L))
				.actionedReports(reportCounts.getOrDefault(CommentReportStatus.ACTIONED, 0L))
				.dismissedReports(reportCounts.getOrDefault(CommentReportStatus.DISMISSED, 0L))
				.reportsLast24Hours(reportsLast24Hours == null ? 0L : reportsLast24Hours)
				.autoFlaggedLast24Hours(autoFlaggedLast24Hours == null ? 0L : autoFlaggedLast24Hours)
				.oldestPendingAt(oldestPendingAt)
				.commentsByStatus(toCommentStatusCounts(commentCounts))
				.reportsByStatus(toReportStatusCounts(reportCounts))
				.moderationActionsLast7Days(moderationActionsLast7Days)
				.build();

		return ApiResponse.success(overview);
	}

	public ApiResponse<PageResult<CommentReportResponse>> retrieveCommentReports(CommentReportStatus status,
			Long commentId, String reporterUsername, Pageable pageable)
	{
		List<Object> filters = new ArrayList<>();
		String whereClause = buildReportWhereClause(status, commentId, reporterUsername, filters);
		Long total = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM blog_comment_report report
				JOIN blog_comment comment ON comment.id = report.comment_id
				JOIN sys_user reporter ON reporter.id = report.reporter_id
				LEFT JOIN blog_post post ON post.id = comment.post_id
				""" + whereClause, Long.class, filters.toArray());

		List<Object> dataArgs = new ArrayList<>(filters);
		dataArgs.add(pageable.getPageSize());
		dataArgs.add(pageable.getOffset());
		List<CommentReportResponse> reports = jdbcTemplate.query("""
				SELECT report.comment_id, report.reporter_id, reporter.username AS reporter_username,
				       reporter.nickname AS reporter_nickname, report.reason, report.description,
				       report.status, report.handled_by, report.handled_at, report.resolution_note,
				       comment.post_id, post.title AS post_title, comment.parent_id,
				       comment.status AS comment_status, comment.content AS comment_content, report.created_at
				FROM blog_comment_report report
				JOIN blog_comment comment ON comment.id = report.comment_id
				JOIN sys_user reporter ON reporter.id = report.reporter_id
				LEFT JOIN blog_post post ON post.id = comment.post_id
				""" + whereClause + """
				ORDER BY report.created_at DESC, report.comment_id DESC
				LIMIT ? OFFSET ?
				""", this::mapReport, dataArgs.toArray());

		return ApiResponse.success(new PageResult<>(reports, total == null ? 0L : total, pageable.getPageNumber() + 1,
				pageable.getPageSize(), calculateTotalPages(total, pageable.getPageSize())));
	}

	public ApiResponse<PageResult<CommentRiskResponse>> retrieveHighRiskComments(Long minOpenReports, Pageable pageable)
	{
		long threshold = minOpenReports == null ? moderationProperties.getHighRiskReportThreshold() : minOpenReports;
		String baseQuery = """
				FROM blog_comment comment
				JOIN sys_user author ON author.id = comment.user_id
				LEFT JOIN blog_post post ON post.id = comment.post_id
				LEFT JOIN blog_comment parent ON parent.id = comment.parent_id
				LEFT JOIN blog_comment_report open_report
				       ON open_report.comment_id = comment.id AND open_report.status = ?
				WHERE comment.is_deleted = FALSE
				GROUP BY comment.id, comment.parent_id, comment.post_id, post.title, comment.content,
				         author.username, author.nickname, author.avatar, comment.status, comment.reports_count,
				         comment.likes_count, comment.created_at, comment.edited_at
				HAVING COUNT(open_report.reporter_id) >= ?
				""";
		Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (SELECT comment.id " + baseQuery + ") high_risk",
				Long.class, CommentReportStatus.OPEN.name(), threshold);
		List<CommentRiskResponse> comments = jdbcTemplate.query("""
				SELECT comment.id, comment.parent_id, comment.post_id, post.title AS post_title, comment.content,
				       author.username, author.nickname, author.avatar, comment.status, comment.reports_count,
				       COUNT(open_report.reporter_id) AS open_reports, comment.likes_count,
				       (COUNT(open_report.reporter_id) * 10 + comment.reports_count * 2) AS risk_score,
				       comment.created_at, comment.edited_at
				""" + baseQuery + """
				ORDER BY open_reports DESC, risk_score DESC, comment.created_at ASC, comment.id ASC
				LIMIT ? OFFSET ?
				""", this::mapRiskComment, CommentReportStatus.OPEN.name(), threshold, pageable.getPageSize(),
				pageable.getOffset());

		return ApiResponse.success(new PageResult<>(comments, total == null ? 0L : total, pageable.getPageNumber() + 1,
				pageable.getPageSize(), calculateTotalPages(total, pageable.getPageSize())));
	}

	public ApiResponse<PageResult<CommentModerationLogResponse>> retrieveCommentModerationLogs(Long commentId,
			CommentModerationAction action, Pageable pageable)
	{
		List<Object> filters = new ArrayList<>();
		String whereClause = buildModerationLogWhereClause(commentId, action, filters);
		Long total = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM blog_comment_moderation_log log
				JOIN blog_comment comment ON comment.id = log.comment_id
				LEFT JOIN blog_post post ON post.id = comment.post_id
				""" + whereClause, Long.class, filters.toArray());

		List<Object> dataArgs = new ArrayList<>(filters);
		dataArgs.add(pageable.getPageSize());
		dataArgs.add(pageable.getOffset());
		List<CommentModerationLogResponse> logs = jdbcTemplate.query("""
				SELECT log.id, log.comment_id, comment.post_id, post.title AS post_title, comment.parent_id,
				       comment.content AS comment_content, log.action, log.previous_status, log.new_status,
				       log.moderator_username, log.reason, log.batch_id, log.note, log.created_at
				FROM blog_comment_moderation_log log
				JOIN blog_comment comment ON comment.id = log.comment_id
				LEFT JOIN blog_post post ON post.id = comment.post_id
				""" + whereClause + """
				ORDER BY log.created_at DESC, log.id DESC
				LIMIT ? OFFSET ?
				""", this::mapModerationLog, dataArgs.toArray());

		return ApiResponse.success(new PageResult<>(logs, total == null ? 0L : total, pageable.getPageNumber() + 1,
				pageable.getPageSize(), calculateTotalPages(total, pageable.getPageSize())));
	}

	public ApiResponse<Integer> repairCommentCounters()
	{
		int updated = jdbcTemplate.update("""
				UPDATE blog_comment comment
				SET likes_count = (
				    SELECT COUNT(*) FROM blog_comment_like likes WHERE likes.comment_id = comment.id
				),
				reports_count = (
				    SELECT COUNT(*) FROM blog_comment_report report WHERE report.comment_id = comment.id
				)
				WHERE comment.is_deleted = FALSE
				""");
		return ApiResponse.success("Comment counters repaired.", updated);
	}

	private String buildReportWhereClause(CommentReportStatus status, Long commentId, String reporterUsername,
			List<Object> args)
	{
		List<String> conditions = new ArrayList<>();
		if (status != null)
		{
			conditions.add("report.status = ?");
			args.add(status.name());
		}
		if (commentId != null)
		{
			conditions.add("report.comment_id = ?");
			args.add(commentId);
		}
		if (StringUtils.hasText(reporterUsername))
		{
			conditions.add("reporter.username = ?");
			args.add(reporterUsername.trim());
		}
		return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + "\n";
	}

	private String buildModerationLogWhereClause(Long commentId, CommentModerationAction action, List<Object> args)
	{
		List<String> conditions = new ArrayList<>();
		conditions.add("log.is_deleted = FALSE");
		if (commentId != null)
		{
			conditions.add("log.comment_id = ?");
			args.add(commentId);
		}
		if (action != null)
		{
			conditions.add("log.action = ?");
			args.add(action.name());
		}
		return "WHERE " + String.join(" AND ", conditions) + "\n";
	}

	private CommentReportResponse mapReport(ResultSet rs, int rowNum) throws SQLException
	{
		return CommentReportResponse.builder()
				.commentId(rs.getLong("comment_id"))
				.reporterId(rs.getLong("reporter_id"))
				.reporterUsername(rs.getString("reporter_username"))
				.reporterNickname(rs.getString("reporter_nickname"))
				.reason(rs.getString("reason"))
				.description(rs.getString("description"))
				.status(CommentReportStatus.valueOf(rs.getString("status")))
				.handledBy(rs.getString("handled_by"))
				.handledAt(toLocalDateTime(rs.getTimestamp("handled_at")))
				.resolutionNote(rs.getString("resolution_note"))
				.postId(readNullableLong(rs, "post_id"))
				.postTitle(rs.getString("post_title"))
				.parentId(readNullableLong(rs, "parent_id"))
				.commentStatus(CommentStatus.valueOf(rs.getString("comment_status")))
				.commentContent(rs.getString("comment_content"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.build();
	}

	private CommentRiskResponse mapRiskComment(ResultSet rs, int rowNum) throws SQLException
	{
		return CommentRiskResponse.builder()
				.id(rs.getLong("id"))
				.parentId(readNullableLong(rs, "parent_id"))
				.postId(readNullableLong(rs, "post_id"))
				.postTitle(rs.getString("post_title"))
				.content(rs.getString("content"))
				.username(rs.getString("username"))
				.nickname(rs.getString("nickname"))
				.avatar(rs.getString("avatar"))
				.status(CommentStatus.valueOf(rs.getString("status")))
				.reportsCount(rs.getLong("reports_count"))
				.openReports(rs.getLong("open_reports"))
				.likesCount(rs.getLong("likes_count"))
				.riskScore(rs.getLong("risk_score"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.editedAt(toLocalDateTime(rs.getTimestamp("edited_at")))
				.build();
	}

	private Map<CommentStatus, Long> retrieveCommentStatusCounts()
	{
		Map<CommentStatus, Long> counts = new EnumMap<>(CommentStatus.class);
		jdbcTemplate.query(
				"SELECT status, COUNT(*) AS item_count FROM blog_comment WHERE is_deleted = FALSE GROUP BY status",
				rs ->
				{
					counts.put(CommentStatus.valueOf(rs.getString("status")), rs.getLong("item_count"));
				});
		return counts;
	}

	private Map<CommentReportStatus, Long> retrieveReportStatusCounts()
	{
		Map<CommentReportStatus, Long> counts = new EnumMap<>(CommentReportStatus.class);
		jdbcTemplate.query("SELECT status, COUNT(*) AS item_count FROM blog_comment_report GROUP BY status", rs ->
		{
			counts.put(CommentReportStatus.valueOf(rs.getString("status")), rs.getLong("item_count"));
		});
		return counts;
	}

	private List<CommentGovernanceOverviewResponse.ModerationActionCount> retrieveModerationActionCounts(
			LocalDateTime since)
	{
		return jdbcTemplate.query(
				"SELECT action, COUNT(*) AS item_count FROM blog_comment_moderation_log WHERE is_deleted = FALSE AND created_at >= ? GROUP BY action",
				(rs, rowNum) -> CommentGovernanceOverviewResponse.ModerationActionCount.builder()
						.action(CommentModerationAction.valueOf(rs.getString("action")))
						.count(rs.getLong("item_count"))
						.build(),
				since);
	}

	private List<CommentGovernanceOverviewResponse.CommentStatusCount> toCommentStatusCounts(
			Map<CommentStatus, Long> counts)
	{
		List<CommentGovernanceOverviewResponse.CommentStatusCount> result = new ArrayList<>();
		for (CommentStatus status : CommentStatus.values())
		{
			result.add(CommentGovernanceOverviewResponse.CommentStatusCount.builder()
					.status(status)
					.count(counts.getOrDefault(status, 0L))
					.build());
		}
		return result;
	}

	private List<CommentGovernanceOverviewResponse.ReportStatusCount> toReportStatusCounts(
			Map<CommentReportStatus, Long> counts)
	{
		List<CommentGovernanceOverviewResponse.ReportStatusCount> result = new ArrayList<>();
		for (CommentReportStatus status : CommentReportStatus.values())
		{
			result.add(CommentGovernanceOverviewResponse.ReportStatusCount.builder()
					.status(status)
					.count(counts.getOrDefault(status, 0L))
					.build());
		}
		return result;
	}

	private CommentModerationLogResponse mapModerationLog(ResultSet rs, int rowNum) throws SQLException
	{
		return CommentModerationLogResponse.builder()
				.id(rs.getLong("id"))
				.commentId(rs.getLong("comment_id"))
				.postId(readNullableLong(rs, "post_id"))
				.postTitle(rs.getString("post_title"))
				.parentId(readNullableLong(rs, "parent_id"))
				.commentContent(rs.getString("comment_content"))
				.action(CommentModerationAction.valueOf(rs.getString("action")))
				.previousStatus(readNullableCommentStatus(rs, "previous_status"))
				.newStatus(readNullableCommentStatus(rs, "new_status"))
				.moderatorUsername(rs.getString("moderator_username"))
				.reason(rs.getString("reason"))
				.batchId(rs.getString("batch_id"))
				.note(rs.getString("note"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.build();
	}

	private Long readNullableLong(ResultSet rs, String columnName) throws SQLException
	{
		long value = rs.getLong(columnName);
		return rs.wasNull() ? null : value;
	}

	private CommentStatus readNullableCommentStatus(ResultSet rs, String columnName) throws SQLException
	{
		String value = rs.getString(columnName);
		return value == null ? null : CommentStatus.valueOf(value);
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp)
	{
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private LocalDateTime queryLocalDateTime(String sql, Object... args)
	{
		Timestamp timestamp = jdbcTemplate.queryForObject(sql, Timestamp.class, args);
		return toLocalDateTime(timestamp);
	}

	private long sum(Iterable<Long> values)
	{
		long total = 0L;
		for (Long value : values)
		{
			total += value == null ? 0L : value;
		}
		return total;
	}

	private int calculateTotalPages(Long total, int pageSize)
	{
		if (total == null || total == 0L)
		{
			return 0;
		}
		return (int) Math.ceil((double) total / pageSize);
	}
}
