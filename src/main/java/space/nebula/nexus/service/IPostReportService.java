package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostReportStatus;
import space.nebula.nexus.payload.request.PostReportRequest;
import space.nebula.nexus.payload.request.PostReportResolutionRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostReportResponse;

/**
 * Manages reader reports and administrative resolution for published posts.
 */
public interface IPostReportService {

	/** Records one idempotent report from the current user for a public post. */
	ApiResponse<Void> reportPost(Long postId, PostReportRequest request);

	/** Retrieves the administrative post-report queue. */
	ApiResponse<PageResult<PostReportResponse>> retrieveReports(PostReportStatus status, Long postId,
			String reporterUsername, Pageable pageable);

	/** Resolves one open post report as actioned or dismissed. */
	ApiResponse<Void> resolveReport(Long postId, Long reporterId, PostReportResolutionRequest request);
}
