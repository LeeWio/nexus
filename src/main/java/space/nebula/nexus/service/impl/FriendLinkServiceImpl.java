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
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.service.IFriendLinkService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements IFriendLinkService {

    private final FriendLinkRepository friendLinkRepository;
    private final FriendLinkMapper friendLinkMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<FriendLinkResponse>> getAdminFriendLinks(Pageable pageable) {
        Page<FriendLinkResponse> page = friendLinkRepository.findAll(pageable).map(friendLinkMapper::toResponse);
        return ApiResponse.success(PageResult.of(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<FriendLinkResponse> getFriendLinkById(Long id) {
        return friendLinkRepository.findById(id)
                .map(friendLink -> ApiResponse.success(friendLinkMapper.toResponse(friendLink)))
                .orElseThrow(() -> new BusinessException(404, "Friend link not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Create Friend Link")
    public ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request) {
        FriendLink friendLink = friendLinkMapper.toEntity(request);
        friendLinkRepository.save(friendLink);
        log.info("Friend link created: {}", friendLink.getName());
        return ApiResponse.success("Friend link created successfully", friendLinkMapper.toResponse(friendLink));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Update Friend Link")
    public ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request) {
        FriendLink friendLink = friendLinkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Friend link not found"));

        friendLink.setName(request.name());
        friendLink.setUrl(request.url());
        friendLink.setAvatar(request.avatar());
        friendLink.setDescription(request.description());
        friendLink.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        friendLink.setIsPublished(request.isPublished());

        friendLinkRepository.save(friendLink);
        log.info("Friend link updated: {}", friendLink.getName());
        return ApiResponse.success("Friend link updated successfully", friendLinkMapper.toResponse(friendLink));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Delete Friend Link")
    public ApiResponse<Void> deleteFriendLink(Long id) {
        if (!friendLinkRepository.existsById(id)) {
            throw new BusinessException(404, "Friend link not found");
        }
        friendLinkRepository.deleteById(id);
        log.info("Friend link deleted id: {}", id);
        return ApiResponse.success("Friend link deleted successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "friendLinks", key = "'public_list'")
    public ApiResponse<List<FriendLinkResponse>> getPublicFriendLinks() {
        List<FriendLink> friendLinks = friendLinkRepository.findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();
        return ApiResponse.success(friendLinkMapper.toResponseList(friendLinks));
    }
}
