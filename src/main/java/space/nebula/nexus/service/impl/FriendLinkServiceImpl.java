package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.config.FriendLinkProperties;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.request.FriendLinkApplicationRequest;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.service.IFriendLinkService;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Implementation of friend link management service. Handles public applications
 * and administrative moderation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements IFriendLinkService {

	private final FriendLinkRepository friendLinkRepository;
	private final FriendLinkMapper friendLinkMapper;
	private final RabbitTemplate rabbitTemplate;
	private final FriendLinkProperties friendLinkProperties;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<FriendLinkResponse>> retrieveAdminFriendLinks(Pageable pageable) {
		var friendLinkPage = friendLinkRepository.findAll(pageable).map(friendLinkMapper::toResponse);
		return ApiResponse.success(PageResult.of(friendLinkPage));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<FriendLinkResponse> retrieveFriendLinkById(Long id) {
		var friendLink = findFriendLinkOrThrow(id);
		return ApiResponse.success(friendLinkMapper.toResponse(friendLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Create Friend Link")
	public ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request) {
		String normalizedUrl = normalizeHttpUrl(request.url(), "Site URL");
		validateUrlUniqueness(normalizedUrl, null);

		var newLink = friendLinkMapper.toEntity(request);
		newLink.setUrl(normalizedUrl);
		newLink.setAvatar(normalizeOptionalHttpUrl(request.avatar(), "Avatar URL"));
		newLink.setStatus(FriendLinkStatus.APPROVED);
		newLink.setIsPublished(true);

		var savedLink = friendLinkRepository.save(newLink);
		log.info("New friend link created: {}", savedLink.getName());
		return ApiResponse.success("Friend link created successfully", friendLinkMapper.toResponse(savedLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Update Friend Link")
	public ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request) {
		var existingLink = findFriendLinkOrThrow(id);
		String normalizedUrl = normalizeHttpUrl(request.url(), "Site URL");
		validateUrlUniqueness(normalizedUrl, id);

		friendLinkMapper.updateEntity(existingLink, request);
		existingLink.setUrl(normalizedUrl);
		existingLink.setAvatar(normalizeOptionalHttpUrl(request.avatar(), "Avatar URL"));
		var updatedLink = friendLinkRepository.save(existingLink);

		log.info("Friend link updated: {}", updatedLink.getName());
		return ApiResponse.success("Friend link updated successfully", friendLinkMapper.toResponse(updatedLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Delete Friend Link")
	public ApiResponse<Void> deleteFriendLink(Long id) {
		Assert.isTrue(friendLinkRepository.existsById(id), () -> new ResourceNotFoundException("FriendLink", "id", id));
		friendLinkRepository.deleteById(id);
		log.info("Friend link deleted ID: {}", id);
		return ApiResponse.success("Friend link deleted successfully.", null);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.FRIEND_LINKS, key = CacheConstants.PUBLIC_LIST_KEY)
	public ApiResponse<List<FriendLinkResponse>> retrievePublicFriendLinks() {
		var activeLinks = friendLinkRepository
				.findByStatusAndIsPublishedTrueOrderBySortOrderAscCreatedAtDesc(FriendLinkStatus.APPROVED);
		return ApiResponse.success(friendLinkMapper.toResponseList(activeLinks));
	}

	@Override
	@Transactional
	@LogOperation("Apply for Friend Link")
	public ApiResponse<Void> applyForFriendLink(FriendLinkApplicationRequest request) {
		String normalizedUrl = normalizeHttpUrl(request.url(), "Site URL");
		validateUrlUniqueness(normalizedUrl, null);

		var application = new FriendLink();
		application.setName(request.name().trim());
		application.setUrl(normalizedUrl);
		application.setAvatar(normalizeOptionalHttpUrl(request.avatar(), "Avatar URL"));
		application.setDescription(StrUtil.trim(request.description()));
		application.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		application.setStatus(FriendLinkStatus.APPLYING);
		application.setIsPublished(false);
		application.setSortOrder(0);

		friendLinkRepository.save(application);
		log.info("Received friend link application: {}", application.getUrl());

		sendApplicationNotification(application);

		return ApiResponse.success("Application submitted successfully. It will be reviewed.", null);
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Moderate Friend Link")
	public ApiResponse<Void> moderateFriendLink(Long id, FriendLinkStatus status) {
		var link = findFriendLinkOrThrow(id);
		Assert.isTrue(link.getStatus() == FriendLinkStatus.APPLYING,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only pending applications can be moderated"));
		Assert.isTrue(status == FriendLinkStatus.APPROVED || status == FriendLinkStatus.REJECTED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Moderation result must be APPROVED or REJECTED"));
		link.setStatus(status);
		link.setIsPublished(status == FriendLinkStatus.APPROVED);
		friendLinkRepository.save(link);
		log.info("Friend link ID {} status updated to {}", id, status);
		return ApiResponse.success("Friend link status updated.", null);
	}

	private FriendLink findFriendLinkOrThrow(Long id) {
		return friendLinkRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("FriendLink", "id", id));
	}

	private void validateUrlUniqueness(String url, Long excludeId) {
		friendLinkRepository.findByUrl(url).ifPresent(link -> {
			Assert.isFalse(excludeId == null || !link.getId().equals(excludeId),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Friend link URL is already in use"));
		});
	}

	private void sendApplicationNotification(FriendLink link) {
		if (StrUtil.isBlank(friendLinkProperties.getModerationEmail())) {
			log.warn("Friend-link moderation email is not configured; application {} remains queued", link.getId());
			return;
		}
		String subject = "New Friend Link Application: " + link.getName();
		String content = StrUtil.format(
				"Hello Admin,\n\nA new friend link application has been submitted:\n\n"
						+ "Site Name: {}\nSite URL: {}\nDescription: {}\nContact Email: {}\n\n"
						+ "Please log in to moderate this application.",
				link.getName(), link.getUrl(), link.getDescription(), link.getEmail());

		TemplateMailMessage message = TemplateMailMessage.builder().to(friendLinkProperties.getModerationEmail())
				.subject(subject).content(content).type(TemplateMailMessage.MailType.SIMPLE).build();

		try {
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
		} catch (RuntimeException e) {
			log.error("Failed to enqueue moderation notification for friend-link application {}", link.getId(), e);
		}
	}

	private String normalizeOptionalHttpUrl(String value, String fieldName) {
		return StrUtil.isBlank(value) ? null : normalizeHttpUrl(value, fieldName);
	}

	private String normalizeHttpUrl(String value, String fieldName) {
		try {
			URI uri = new URI(value.trim());
			String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
			Assert.isTrue(
					("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null
							&& uri.getUserInfo() == null,
					() -> new BusinessException(BusinessCode.BAD_REQUEST,
							fieldName + " must be an absolute HTTP or HTTPS URL without embedded credentials"));
			URI normalized = new URI(scheme, null, uri.getHost().toLowerCase(Locale.ROOT), uri.getPort(), uri.getPath(),
					uri.getQuery(), null).normalize();
			String result = normalized.toASCIIString();
			return result.endsWith("/") && "/".equals(normalized.getPath())
					? result.substring(0, result.length() - 1)
					: result;
		} catch (URISyntaxException | IllegalArgumentException e) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, fieldName + " is invalid");
		}
	}
}
