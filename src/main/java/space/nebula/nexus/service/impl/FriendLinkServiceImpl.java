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
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.service.IFriendLinkService;
import space.nebula.nexus.utils.MailUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements IFriendLinkService {

    private final FriendLinkRepository friendLinkRepository;
    private final FriendLinkMapper friendLinkMapper;
    private final MailUtil mailUtil;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<FriendLinkResponse>> retrieveAdminFriendLinks(Pageable pageable) {
        Page<FriendLinkResponse> friendLinkPage = friendLinkRepository.findAll(pageable).map(friendLinkMapper::toResponse);
        return ApiResponse.success(PageResult.of(friendLinkPage));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<FriendLinkResponse> retrieveFriendLinkById(Long id) {
        FriendLink friendLink = findFriendLinkOrThrow(id);
        return ApiResponse.success(friendLinkMapper.toResponse(friendLink));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Create Friend Link")
    public ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request) {
        validateUrlUniqueness(request.url(), null);
        
        FriendLink newLink = friendLinkMapper.toEntity(request);
        if (newLink.getStatus() == null) newLink.setStatus(FriendLinkStatus.APPROVED);
        
        friendLinkRepository.save(newLink);
        log.info("New friend link created: {}", newLink.getName());
        return ApiResponse.success("Friend link created successfully", friendLinkMapper.toResponse(newLink));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Update Friend Link")
    public ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request) {
        FriendLink existingLink = findFriendLinkOrThrow(id);
        validateUrlUniqueness(request.url(), id);

        friendLinkMapper.updateEntity(existingLink, request);
        friendLinkRepository.save(existingLink);

        log.info("Friend link updated: {}", existingLink.getName());
        return ApiResponse.success("Friend link updated successfully", friendLinkMapper.toResponse(existingLink));
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Delete Friend Link")
    public ApiResponse<Void> deleteFriendLink(Long id) {
        if (!friendLinkRepository.existsById(id)) {
            throw new ResourceNotFoundException("FriendLink", "id", id);
        }
        friendLinkRepository.deleteById(id);
        log.info("Friend link deleted ID: {}", id);
        return ApiResponse.success();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "friendLinks", key = "'public_list'")
    public ApiResponse<List<FriendLinkResponse>> retrievePublicFriendLinks() {
        List<FriendLink> activeLinks = friendLinkRepository.findByStatusAndIsPublishedTrueOrderBySortOrderAscCreatedAtDesc(FriendLinkStatus.APPROVED);
        return ApiResponse.success(friendLinkMapper.toResponseList(activeLinks));
    }

    @Override
    @Transactional
    @LogOperation("Apply for Friend Link")
    public ApiResponse<Void> applyForFriendLink(FriendLinkRequest request) {
        validateUrlUniqueness(request.url(), null);

        FriendLink application = friendLinkMapper.toEntity(request);
        application.setStatus(FriendLinkStatus.APPLYING);
        application.setIsPublished(false);
        application.setSortOrder(0);

        friendLinkRepository.save(application);
        log.info("Received friend link application: {}", application.getUrl());

        // Notify admin via email
        sendApplicationNotification(application);

        return ApiResponse.success("Application submitted successfully. It will be reviewed by the administrator.", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "friendLinks", allEntries = true)
    @LogOperation("Moderate Friend Link")
    public ApiResponse<Void> moderateFriendLink(Long id, FriendLinkStatus status) {
        FriendLink link = findFriendLinkOrThrow(id);
        link.setStatus(status);
        if (status == FriendLinkStatus.APPROVED) {
            link.setIsPublished(true);
        }
        friendLinkRepository.save(link);
        log.info("Friend link ID {} status updated to {}", id, status);
        return ApiResponse.success("Friend link status updated to " + status, null);
    }

    private FriendLink findFriendLinkOrThrow(Long id) {
        return friendLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FriendLink", "id", id));
    }

    private void validateUrlUniqueness(String url, Long excludeId) {
        friendLinkRepository.findByUrl(url).ifPresent(link -> {
            if (excludeId == null || !link.getId().equals(excludeId)) {
                throw new BusinessException("Friend link with this URL already exists");
            }
        });
    }

    private void sendApplicationNotification(FriendLink link) {
        String subject = "New Friend Link Application: " + link.getName();
        String content = String.format(
                "Hello Admin,\n\nA new friend link application has been submitted:\n\n" +
                "Site Name: %s\n" +
                "Site URL: %s\n" +
                "Description: %s\n" +
                "Contact Email: %s\n\n" +
                "Please log in to the admin panel to moderate this application.",
                link.getName(), link.getUrl(), link.getDescription(), link.getEmail()
        );
        // Fallback email if no admin-email property, assuming admin@nexus.com
        mailUtil.sendSimpleMail("admin@nexus.com", subject, content);
    }
}
