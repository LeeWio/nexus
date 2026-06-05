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
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.service.IFriendLinkService;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Implementation of friend link management service. Handles public applications
 * and administrative moderation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements IFriendLinkService
{

	private final FriendLinkRepository friendLinkRepository;
	private final FriendLinkMapper friendLinkMapper;
	private final RabbitTemplate rabbitTemplate;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<FriendLinkResponse>> retrieveAdminFriendLinks(Pageable pageable)
	{
		var friendLinkPage = friendLinkRepository.findAll(pageable).map(friendLinkMapper::toResponse);
		return ApiResponse.success(PageResult.of(friendLinkPage));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<FriendLinkResponse> retrieveFriendLinkById(Long id)
	{
		var friendLink = findFriendLinkOrThrow(id);
		return ApiResponse.success(friendLinkMapper.toResponse(friendLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Create Friend Link")
	public ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request)
	{
		validateUrlUniqueness(request.url(), null);

		var newLink = friendLinkMapper.toEntity(request);
		if (newLink.getStatus() == null)
		{
			newLink.setStatus(FriendLinkStatus.APPROVED);
		}

		var savedLink = friendLinkRepository.save(newLink);
		log.info("New friend link created: {}", savedLink.getName());
		return ApiResponse.success("Friend link created successfully", friendLinkMapper.toResponse(savedLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Update Friend Link")
	public ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request)
	{
		var existingLink = findFriendLinkOrThrow(id);
		validateUrlUniqueness(request.url(), id);

		friendLinkMapper.updateEntity(existingLink, request);
		var updatedLink = friendLinkRepository.save(existingLink);

		log.info("Friend link updated: {}", updatedLink.getName());
		return ApiResponse.success("Friend link updated successfully", friendLinkMapper.toResponse(updatedLink));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Delete Friend Link")
	public ApiResponse<Void> deleteFriendLink(Long id)
	{
		Assert.isTrue(friendLinkRepository.existsById(id), () -> new ResourceNotFoundException("FriendLink", "id", id));
		friendLinkRepository.deleteById(id);
		log.info("Friend link deleted ID: {}", id);
		return ApiResponse.success("Friend link deleted", null);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.FRIEND_LINKS, key = CacheConstants.PUBLIC_LIST_KEY)
	public ApiResponse<List<FriendLinkResponse>> retrievePublicFriendLinks()
	{
		var activeLinks = friendLinkRepository
				.findByStatusAndIsPublishedTrueOrderBySortOrderAscCreatedAtDesc(FriendLinkStatus.APPROVED);
		return ApiResponse.success(friendLinkMapper.toResponseList(activeLinks));
	}

	@Override
	@Transactional
	@LogOperation("Apply for Friend Link")
	public ApiResponse<Void> applyForFriendLink(FriendLinkRequest request)
	{
		validateUrlUniqueness(request.url(), null);

		var application = friendLinkMapper.toEntity(request);
		application.setStatus(FriendLinkStatus.APPLYING);
		application.setIsPublished(false);
		application.setSortOrder(0);

		friendLinkRepository.save(application);
		log.info("Received friend link application: {}", application.getUrl());

		sendApplicationNotification(application);

		return ApiResponse.success("Application submitted successfully. It will be reviewed by the administrator.",
				null);
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.FRIEND_LINKS, allEntries = true)
	@LogOperation("Moderate Friend Link")
	public ApiResponse<Void> moderateFriendLink(Long id, FriendLinkStatus status)
	{
		var link = findFriendLinkOrThrow(id);
		link.setStatus(status);
		if (status == FriendLinkStatus.APPROVED)
		{
			link.setIsPublished(true);
		}
		friendLinkRepository.save(link);
		log.info("Friend link ID {} status updated to {}", id, status);
		return ApiResponse.success("Friend link status updated to " + status, null);
	}

	private FriendLink findFriendLinkOrThrow(Long id)
	{
		return friendLinkRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("FriendLink", "id", id));
	}

	private void validateUrlUniqueness(String url, Long excludeId)
	{
		friendLinkRepository.findByUrl(url).ifPresent(link ->
		{
			Assert.isFalse(excludeId == null || !link.getId().equals(excludeId),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Friend link with this URL already exists"));
		});
	}

	private void sendApplicationNotification(FriendLink link)
	{
		String subject = "New Friend Link Application: " + link.getName();
		String content = StrUtil.format(
				"Hello Admin,\n\nA new friend link application has been submitted:\n\n"
						+ "Site Name: {}\nSite URL: {}\nDescription: {}\nContact Email: {}\n\n"
						+ "Please log in to moderate this application.",
				link.getName(), link.getUrl(), link.getDescription(), link.getEmail());

		TemplateMailMessage message = TemplateMailMessage.builder().to("admin@nexus.com").subject(subject)
				.content(content).type(TemplateMailMessage.MailType.SIMPLE).build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
	}
}
