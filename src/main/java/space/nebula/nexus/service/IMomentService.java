package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentCadenceResponse;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.MomentTopicCountResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;
import java.util.Set;

public interface IMomentService {

	// Admin methods
	ApiResponse<PageResult<MomentResponse>> getAdminMoments(Pageable pageable);

	ApiResponse<MomentResponse> getMomentById(Long id);

	ApiResponse<MomentResponse> createMoment(MomentRequest request);

	ApiResponse<MomentResponse> updateMoment(Long id, MomentRequest request);

	ApiResponse<Void> deleteMoment(Long id);

	// Public methods
	ApiResponse<PageResult<MomentResponse>> getPublicMoments(String topic, Integer year, Integer month,
			Pageable pageable);

	ApiResponse<List<MomentTopicCountResponse>> getPublicTopics();

	/**
	 * Returns which current day, month, and year buckets contain a public moment.
	 * 
	 * @return publishing cadence in the author time zone
	 */
	ApiResponse<MomentCadenceResponse> getPublicCadence();

	ApiResponse<Void> likeMoment(Long id);

	ApiResponse<Void> unlikeMoment(Long id);

	ApiResponse<Set<Long>> getLikedMomentIds(List<Long> momentIds);
}
