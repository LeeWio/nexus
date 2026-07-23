package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.tree.Tree;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.payload.response.CommentAnchorContextResponse;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.CommentGovernanceOverviewResponse;
import space.nebula.nexus.payload.response.CommentModerationLogResponse;
import space.nebula.nexus.payload.response.CommentRiskResponse;
import space.nebula.nexus.payload.response.CommentReportResponse;
import space.nebula.nexus.payload.response.CursorPageResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

	private final CommentCommandService commandService;
	private final CommentQueryService queryService;
	private final CommentModerationService moderationService;
	private final CommentGovernanceService governanceService;

	@Override
	public ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest) {
		return commandService.publishComment(request, servletRequest);
	}

	@Override
	public ApiResponse<List<Tree<Long>>> retrieveCommentsByPost(Long postId) {
		return queryService.retrieveCommentsByPost(postId);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveRootCommentsByPost(Long postId, Pageable pageable) {
		return queryService.retrieveRootCommentsByPost(postId, pageable);
	}

	@Override
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRootCommentsByPostCursor(Long postId, Long cursor,
			int size) {
		return queryService.retrieveRootCommentsByPostCursor(postId, cursor, size);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveReplies(Long parentId, Pageable pageable) {
		return queryService.retrieveReplies(parentId, pageable);
	}

	@Override
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRepliesCursor(Long parentId, Long cursor,
			int size) {
		return queryService.retrieveRepliesCursor(parentId, cursor, size);
	}

	@Override
	public ApiResponse<List<Tree<Long>>> retrieveGuestbookComments() {
		return queryService.retrieveGuestbookComments();
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveGuestbookRootComments(Pageable pageable) {
		return queryService.retrieveGuestbookRootComments(pageable);
	}

	@Override
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveGuestbookRootCommentsCursor(Long cursor, int size) {
		return queryService.retrieveGuestbookRootCommentsCursor(cursor, size);
	}

	@Override
	public ApiResponse<Long> countNewRootCommentsByPost(Long postId, Long afterId) {
		return queryService.countNewRootCommentsByPost(postId, afterId);
	}

	@Override
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewRootCommentsByPost(Long postId, Long afterId,
			int size) {
		return queryService.retrieveNewRootCommentsByPost(postId, afterId, size);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveHotRootCommentsByPost(Long postId, Pageable pageable) {
		return queryService.retrieveHotRootCommentsByPost(postId, pageable);
	}

	@Override
	public ApiResponse<CommentAnchorContextResponse> retrieveCommentAnchorContext(Long commentId, int replyWindowSize) {
		return queryService.retrieveCommentAnchorContext(commentId, replyWindowSize);
	}

	@Override
	public ApiResponse<Long> countNewGuestbookRootComments(Long afterId) {
		return queryService.countNewGuestbookRootComments(afterId);
	}

	@Override
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewGuestbookRootComments(Long afterId, int size) {
		return queryService.retrieveNewGuestbookRootComments(afterId, size);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveHotGuestbookRootComments(Pageable pageable) {
		return queryService.retrieveHotGuestbookRootComments(pageable);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrieveMyComments(CommentStatus status, Pageable pageable) {
		return queryService.retrieveMyComments(status, pageable);
	}

	@Override
	public ApiResponse<Void> withdrawMyComment(Long id) {
		return commandService.withdrawMyComment(id);
	}

	@Override
	public ApiResponse<Void> updateMyComment(Long id, CommentUpdateRequest request) {
		return commandService.updateMyComment(id, request);
	}

	@Override
	public ApiResponse<Void> deleteMyComment(Long id) {
		return commandService.deleteMyComment(id);
	}

	@Override
	public ApiResponse<Void> reportComment(Long id, CommentReportRequest request) {
		return commandService.reportComment(id, request);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(CommentStatus status, Long postId,
			String username, String keyword, Pageable pageable) {
		return queryService.searchCommentsForManagement(status, postId, username, keyword, pageable);
	}

	@Override
	public ApiResponse<PageResult<CommentResponse>> retrievePendingComments(Pageable pageable) {
		return queryService.retrievePendingComments(pageable);
	}

	@Override
	public ApiResponse<CommentGovernanceOverviewResponse> retrieveCommentGovernanceOverview() {
		return governanceService.retrieveCommentGovernanceOverview();
	}

	@Override
	public ApiResponse<PageResult<CommentReportResponse>> retrieveCommentReports(CommentReportStatus status,
			Long commentId, String reporterUsername, Pageable pageable) {
		return governanceService.retrieveCommentReports(status, commentId, reporterUsername, pageable);
	}

	@Override
	public ApiResponse<PageResult<CommentRiskResponse>> retrieveHighRiskComments(Long minOpenReports,
			Pageable pageable) {
		return governanceService.retrieveHighRiskComments(minOpenReports, pageable);
	}

	@Override
	public ApiResponse<PageResult<CommentModerationLogResponse>> retrieveCommentModerationLogs(Long commentId,
			CommentModerationAction action, Pageable pageable) {
		return governanceService.retrieveCommentModerationLogs(commentId, action, pageable);
	}

	@Override
	public ApiResponse<Integer> repairCommentCounters() {
		return governanceService.repairCommentCounters();
	}

	@Override
	public ApiResponse<Void> moderateComment(Long id, CommentStatus status) {
		return moderationService.moderateComment(id, status);
	}

	@Override
	public ApiResponse<Integer> batchModerateComments(List<Long> ids, CommentStatus status) {
		return moderationService.batchModerateComments(ids, status);
	}

	@Override
	public ApiResponse<Void> pinComment(Long id, boolean pinned) {
		return moderationService.pinComment(id, pinned);
	}

	@Override
	public ApiResponse<Void> featureComment(Long id, boolean featured) {
		return moderationService.featureComment(id, featured);
	}

	@Override
	public ApiResponse<Void> deleteComment(Long id) {
		return moderationService.deleteComment(id);
	}
}
