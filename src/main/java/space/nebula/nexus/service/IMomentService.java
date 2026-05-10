package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface IMomentService {

    // Admin methods
    ApiResponse<PageResult<MomentResponse>> getAdminMoments(Pageable pageable);
    ApiResponse<MomentResponse> getMomentById(Long id);
    ApiResponse<MomentResponse> createMoment(MomentRequest request);
    ApiResponse<MomentResponse> updateMoment(Long id, MomentRequest request);
    ApiResponse<Void> deleteMoment(Long id);

    // Public methods
    ApiResponse<PageResult<MomentResponse>> getPublicMoments(Pageable pageable);
    ApiResponse<Void> likeMoment(Long id);
}
