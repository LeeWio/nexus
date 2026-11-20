package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;

import java.time.LocalDateTime;

public interface IPostPublicationService {
	ApiResponse<Void> submitForReview(Long id);

	ApiResponse<Void> withdrawFromReview(Long id);

	ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment);

	ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request);

	ApiResponse<Void> cancelScheduledPost(Long id);

	ApiResponse<Void> archivePost(Long id, PostArchiveRequest request);

	ApiResponse<Void> restoreArchivedPost(Long id);

	int publishDueScheduledPosts(LocalDateTime now, int batchSize);
}
