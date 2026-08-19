package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostReportStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostReportRequest;
import space.nebula.nexus.payload.request.PostReportResolutionRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostReportResponse;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IPostReportService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps reader reporting separate from article publication state while giving
 * moderators an auditable queue of report decisions.
 */
@Service
@RequiredArgsConstructor
public class PostReportServiceImpl implements IPostReportService {

	private static final String REPORT_FROM = """
			FROM blog_post_report report
			JOIN blog_post post ON post.id = report.post_id
			JOIN sys_user reporter ON reporter.id = report.reporter_id
			""";

	private final JdbcTemplate jdbcTemplate;
	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public ApiResponse<Void> reportPost(Long postId, PostReportRequest request) {
		User reporter = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(post.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only published posts can be reported"));
		Long postAuthorId = post.getAuthor() == null ? null : post.getAuthor().getId();
		Assert.isFalse(reporter.getId().equals(postAuthorId),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "You cannot report your own post"));

		int inserted = jdbcTemplate.update("""
				INSERT IGNORE INTO blog_post_report
				(post_id, reporter_id, reason, description, status, created_at)
				VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))
				""", postId, reporter.getId(), request.reason().trim(), normalize(request.description()),
				PostReportStatus.OPEN.name());
		return ApiResponse.success(inserted > 0 ? "Post report received." : "Post report was already received.", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostReportResponse>> retrieveReports(PostReportStatus status, Long postId,
			String reporterUsername, Pageable pageable) {
		List<Object> filters = new ArrayList<>();
		String whereClause = buildWhereClause(status, postId, reporterUsername, filters);
		Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + REPORT_FROM + whereClause, Long.class,
				filters.toArray());

		List<Object> dataArgs = new ArrayList<>(filters);
		dataArgs.add(pageable.getPageSize());
		dataArgs.add(pageable.getOffset());
		List<PostReportResponse> reports = jdbcTemplate.query("""
				SELECT report.post_id, post.title AS post_title, post.slug AS post_slug, post.status AS post_status,
				       report.reporter_id, reporter.username AS reporter_username,
				       reporter.nickname AS reporter_nickname, report.reason, report.description, report.status,
				       report.handled_by, report.handled_at, report.resolution_note, report.created_at
				""" + REPORT_FROM + whereClause + """
				ORDER BY report.created_at DESC, report.post_id DESC, report.reporter_id DESC
				LIMIT ? OFFSET ?
				""", this::mapReport, dataArgs.toArray());

		long totalItems = total == null ? 0L : total;
		int totalPages = pageable.getPageSize() == 0
				? 0
				: (int) Math.ceil((double) totalItems / pageable.getPageSize());
		return ApiResponse.success(new PageResult<>(reports, totalItems, pageable.getPageNumber() + 1,
				pageable.getPageSize(), totalPages));
	}

	@Override
	@Transactional
	public ApiResponse<Void> resolveReport(Long postId, Long reporterId, PostReportResolutionRequest request) {
		Assert.notNull(request, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Report resolution is required"));
		Assert.notNull(request.status(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Report status is required"));
		Assert.isTrue(request.status() != PostReportStatus.OPEN,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Reports must be actioned or dismissed"));
		User moderator = SecurityUtil.getCurrentUserOrThrow(userRepository);
		int updated = jdbcTemplate.update("""
				UPDATE blog_post_report
				SET status = ?, resolution_note = ?, handled_by = ?, handled_at = CURRENT_TIMESTAMP(3)
				WHERE post_id = ? AND reporter_id = ? AND status = ?
				""", request.status().name(), normalize(request.resolutionNote()), moderator.getUsername(), postId,
				reporterId, PostReportStatus.OPEN.name());
		if (updated > 0) {
			return ApiResponse.success("Post report resolved.", null);
		}

		Long existing = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM blog_post_report WHERE post_id = ? AND reporter_id = ?", Long.class, postId,
				reporterId);
		if (existing == null || existing == 0) {
			throw new ResourceNotFoundException("PostReport", "postId/reporterId", postId + "/" + reporterId);
		}
		return ApiResponse.success("Post report was already resolved.", null);
	}

	private String buildWhereClause(PostReportStatus status, Long postId, String reporterUsername,
			List<Object> filters) {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		if (status != null) {
			where.append(" AND report.status = ?");
			filters.add(status.name());
		}
		if (postId != null) {
			where.append(" AND report.post_id = ?");
			filters.add(postId);
		}
		if (StringUtils.hasText(reporterUsername)) {
			where.append(" AND LOWER(reporter.username) LIKE ?");
			filters.add("%" + reporterUsername.trim().toLowerCase(Locale.ROOT) + "%");
		}
		return where.toString();
	}

	private PostReportResponse mapReport(ResultSet rs, int rowNum) throws SQLException {
		return new PostReportResponse(rs.getLong("post_id"), rs.getString("post_title"), rs.getString("post_slug"),
				PostStatus.valueOf(rs.getString("post_status")), rs.getLong("reporter_id"),
				rs.getString("reporter_username"), rs.getString("reporter_nickname"), rs.getString("reason"),
				rs.getString("description"), PostReportStatus.valueOf(rs.getString("status")),
				rs.getString("handled_by"), toLocalDateTime(rs.getTimestamp("handled_at")),
				rs.getString("resolution_note"), toLocalDateTime(rs.getTimestamp("created_at")));
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
