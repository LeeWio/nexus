package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.mapper.MomentMapper;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.service.IMomentService;

import space.nebula.nexus.common.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements IMomentService {

    private final MomentRepository momentRepository;
    private final MomentMapper momentMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<MomentResponse>> getAdminMoments(Pageable pageable) {
        Page<MomentResponse> page = momentRepository.findAll(pageable).map(momentMapper::toResponse);
        return ApiResponse.success(PageResult.of(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MomentResponse> getMomentById(Long id) {
        Moment moment = findMomentOrThrow(id);
        return ApiResponse.success(momentMapper.toResponse(moment));
    }

    @Override
    @Transactional
    @CacheEvict(value = "moments", allEntries = true)
    @LogOperation("Create Moment")
    public ApiResponse<MomentResponse> createMoment(MomentRequest request) {
        Moment moment = momentMapper.toEntity(request);
        momentRepository.save(moment);
        log.info("Moment created");
        return ApiResponse.success("Moment created successfully", momentMapper.toResponse(moment));
    }

    @Override
    @Transactional
    @CacheEvict(value = "moments", allEntries = true)
    @LogOperation("Update Moment")
    public ApiResponse<MomentResponse> updateMoment(Long id, MomentRequest request) {
        Moment moment = findMomentOrThrow(id);
        momentMapper.updateEntity(moment, request);
        momentRepository.save(moment);

        log.info("Moment updated: {}", id);
        return ApiResponse.success("Moment updated successfully", momentMapper.toResponse(moment));
    }

    @Override
    @Transactional
    @CacheEvict(value = "moments", allEntries = true)
    @LogOperation("Delete Moment")
    public ApiResponse<Void> deleteMoment(Long id) {
        if (!momentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Moment", "id", id);
        }
        momentRepository.deleteById(id);
        log.info("Moment deleted: {}", id);
        return ApiResponse.success("Moment deleted successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "moments", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public ApiResponse<PageResult<MomentResponse>> getPublicMoments(Pageable pageable) {
        Page<MomentResponse> page = momentRepository.findByIsPublishedTrueOrderByCreatedAtDesc(pageable).map(momentMapper::toResponse);
        return ApiResponse.success(PageResult.of(page));
    }

    @Override
    @Transactional
    @CacheEvict(value = "moments", allEntries = true)
    public ApiResponse<Void> likeMoment(Long id) {
        Moment moment = findMomentOrThrow(id);
        moment.setLikesCount(moment.getLikesCount() + 1);
        momentRepository.save(moment);
        return ApiResponse.success("Moment liked", null);
    }

    private Moment findMomentOrThrow(Long id) {
        return momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moment", "id", id));
    }
}
