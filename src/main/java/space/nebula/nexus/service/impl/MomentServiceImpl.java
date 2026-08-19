package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.MomentMedia;
import space.nebula.nexus.entity.MomentTopic;
import space.nebula.nexus.entity.MomentTopicRelation;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.mapper.MomentMapper;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.MomentTopicRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IMomentService;
import space.nebula.nexus.utils.MomentContentPolicy;
import space.nebula.nexus.utils.MomentTopicPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements IMomentService {
	private static final int MAX_LIKED_MOMENT_IDS = 100;

	private final MomentRepository momentRepository;
	private final MomentTopicRepository momentTopicRepository;
	private final MomentMapper momentMapper;
	private final FileRepository fileRepository;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;

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
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	@LogOperation("Create Moment")
	public ApiResponse<MomentResponse> createMoment(MomentRequest request) {
		validateMomentContent(request.content(), request.images());
		Moment moment = momentMapper.toEntity(request);
		moment.setUser(SecurityUtil.getCurrentUserOrThrow(userRepository));
		replaceImages(moment, request.images(), false);
		replaceTopics(moment, request.topicSlugs(), false);
		momentRepository.save(moment);
		log.info("Moment created");
		return ApiResponse.success("Moment created successfully", momentMapper.toResponse(moment));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	@LogOperation("Update Moment")
	public ApiResponse<MomentResponse> updateMoment(Long id, MomentRequest request) {
		Moment moment = findMomentOrThrow(id);
		validateMomentContent(request.content(), request.images() == null ? moment.getImages() : request.images());
		momentMapper.updateEntity(moment, request);
		if (request.images() != null) {
			replaceImages(moment, request.images(), true);
		}
		if (request.topicSlugs() != null) {
			replaceTopics(moment, request.topicSlugs(), true);
		}
		momentRepository.save(moment);

		log.info("Moment updated: {}", id);
		return ApiResponse.success("Moment updated successfully", momentMapper.toResponse(moment));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	@LogOperation("Delete Moment")
	public ApiResponse<Void> deleteMoment(Long id) {
		Assert.isTrue(momentRepository.existsById(id), () -> new ResourceNotFoundException("Moment", "id", id));
		momentRepository.deleteById(id);
		log.info("Moment deleted: {}", id);
		return ApiResponse.success("Moment deleted successfully", null);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.MOMENTS, key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + T(space.nebula.nexus.security.util.SecurityUtil).getCurrentUsername()")
	public ApiResponse<PageResult<MomentResponse>> getPublicMoments(Pageable pageable) {
		String currentUsername = SecurityUtil.getCurrentUsername();
		Page<MomentResponse> page = momentRepository.findPublicTimeline(MomentVisibility.PUBLIC, currentUsername, pageable)
				.map(momentMapper::toResponse);
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	public ApiResponse<Void> likeMoment(Long id) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		findPublishedMomentOrThrow(id);
		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_moment_like(moment_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				id, user.getId());
		if (inserted > 0) {
			momentRepository.incrementLikes(id, 1L);
		}
		return ApiResponse.success("Moment liked", null);
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	public ApiResponse<Void> unlikeMoment(Long id) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		findMomentOrThrow(id);
		int deleted = jdbcTemplate.update("DELETE FROM blog_moment_like WHERE moment_id = ? AND user_id = ?", id,
				user.getId());
		if (deleted > 0) {
			momentRepository.incrementLikes(id, -1L);
		}
		return ApiResponse.success("Moment like removed", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<Set<Long>> getLikedMomentIds(List<Long> momentIds) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Assert.isTrue(momentIds != null && !momentIds.isEmpty(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment IDs are required"));
		Assert.isTrue(momentIds.size() <= MAX_LIKED_MOMENT_IDS,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "At most 100 moment IDs can be requested"));
		Assert.isTrue(momentIds.stream().allMatch(id -> id != null && id > 0),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment IDs must be positive"));

		Set<Long> uniqueMomentIds = new LinkedHashSet<>(momentIds);
		Set<Long> likedMomentIds = new LinkedHashSet<>(
				momentRepository.findLikedMomentIdsByUserIdAndMomentIdIn(user.getId(), uniqueMomentIds));
		return ApiResponse.success(likedMomentIds);
	}

	private Moment findMomentOrThrow(Long id) {
		return momentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Moment", "id", id));
	}

	private Moment findPublishedMomentOrThrow(Long id) {
		Moment moment = findMomentOrThrow(id);
		String currentUsername = SecurityUtil.getCurrentUsername();
		boolean isVisible = moment.getVisibility() == MomentVisibility.PUBLIC
				|| (currentUsername != null && currentUsername.equals(moment.getCreatedBy()));
		Assert.isTrue(isVisible,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only visible moments can be interacted with"));
		return moment;
	}

	private void validateMomentContent(String content, List<?> images) {
		int characterCount = MomentContentPolicy.visibleCharacterCount(content);
		Assert.isTrue(characterCount <= MomentContentPolicy.MAX_VISIBLE_CHARACTERS,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Moment content must not exceed " + MomentContentPolicy.MAX_VISIBLE_CHARACTERS + " characters"));
		Assert.isTrue(MomentContentPolicy.hasVisibleText(content) || (images != null && !images.isEmpty()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"A moment must include text or at least one image"));
	}

	private void replaceImages(Moment moment,
			List<space.nebula.nexus.payload.request.MomentImageRequest> requestedImages, boolean flushExisting) {
		List<space.nebula.nexus.payload.request.MomentImageRequest> images = requestedImages == null
				? List.of()
				: List.copyOf(requestedImages);
		Assert.isTrue(images.size() <= 9,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A moment can contain at most 9 images"));

		Set<Long> requestedIds = new LinkedHashSet<>();
		for (var image : images) {
			Assert.notNull(image, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment image cannot be null"));
			Assert.isTrue(org.springframework.util.StringUtils.hasText(image.altText()),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment image alt text is required"));
			Assert.isTrue(requestedIds.add(image.fileId()),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment images must be unique"));
		}

		Map<Long, FileMetadata> filesById = new LinkedHashMap<>();
		if (!requestedIds.isEmpty()) {
			fileRepository.findAllById(requestedIds).forEach(file -> filesById.put(file.getId(), file));
			Assert.isTrue(filesById.size() == requestedIds.size(),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more image files do not exist"));
		}

		List<MomentMedia> replacements = new ArrayList<>(images.size());
		for (int index = 0; index < images.size(); index++) {
			var image = images.get(index);
			FileMetadata file = filesById.get(image.fileId());
			Assert.isTrue(Boolean.FALSE.equals(file.getIsDeleted()),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Deleted files cannot be attached"));
			Assert.isTrue(file.getFileType() != null && file.getFileType().startsWith("image/"),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only image files can be attached"));

			MomentMedia media = new MomentMedia();
			media.setMoment(moment);
			media.setFile(file);
			media.setSortOrder(index);
			media.setAltText(image.altText().trim());
			replacements.add(media);
		}

		moment.getImages().clear();
		if (flushExisting && moment.getId() != null) {
			momentRepository.flush();
		}
		moment.getImages().addAll(replacements);
	}

	private void replaceTopics(Moment moment, List<String> requestedSlugs, boolean flushExisting) {
		List<String> topicSlugs = MomentTopicPolicy.normalizeTopicSlugs(requestedSlugs);
		moment.getTopicRelations().clear();
		if (flushExisting && moment.getId() != null) {
			momentRepository.flush();
		}

		for (int index = 0; index < topicSlugs.size(); index++) {
			MomentTopicRelation relation = new MomentTopicRelation();
			relation.setMoment(moment);
			relation.setTopic(findOrCreateTopic(topicSlugs.get(index)));
			relation.setSortOrder(index);
			moment.getTopicRelations().add(relation);
		}
	}

	private MomentTopic findOrCreateTopic(String slug) {
		return momentTopicRepository.findBySlug(slug).orElseGet(() -> {
			MomentTopic topic = new MomentTopic();
			topic.setSlug(slug);
			return momentTopicRepository.save(topic);
		});
	}
}
