package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.PostRevisionMapper;
import space.nebula.nexus.payload.response.PostDiffResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionDetailResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.payload.response.PostRevisionSnapshot;
import space.nebula.nexus.payload.response.PostRevisionSummaryResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostRevisionRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.utils.PostContentAnalyzer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stores immutable article checkpoints and provides safe history operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostRevisionServiceImpl implements IPostRevisionService {

	private final PostRevisionRepository postRevisionRepository;
	private final PostRevisionMapper postRevisionMapper;
	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final PostSeriesRepository seriesRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void saveRevision(Post post, PostRevisionKind revisionKind, String changeSummary) {
		Assert.notNull(post, () -> new BusinessException(BusinessCode.BAD_REQUEST, "A post is required"));
		Assert.notNull(post.getId(), () -> new BusinessException(BusinessCode.BAD_REQUEST, "Post ID is required"));
		Post lockedPost = findPostForUpdateOrThrow(post.getId());
		persistRevision(lockedPost, revisionKind, changeSummary, null);
	}

	@Override
	@Transactional(readOnly = true)
	public void assertExpectedRevision(Long postId, Integer expectedRevisionNumber) {
		if (expectedRevisionNumber == null) {
			return;
		}
		Assert.isTrue(expectedRevisionNumber >= 0,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Expected revision must not be negative"));
		int currentRevisionNumber = postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(postId)
				.map(PostRevision::getVersionNumber).orElse(0);
		Assert.isTrue(expectedRevisionNumber == currentRevisionNumber,
				() -> new BusinessException(409, "Post has revision " + currentRevisionNumber
						+ ". Refresh the article and merge your changes before saving."));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<PostRevisionResponse>> getPostRevisions(Long postId) {
		assertPostExists(postId);
		List<PostRevision> revisions = postRevisionRepository.findByPostIdOrderByVersionNumberDesc(postId);
		return ApiResponse.success(postRevisionMapper.toResponseList(revisions));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<PostRevisionSummaryResponse>> getPostRevisionSummaries(Long postId) {
		assertPostExists(postId);
		return ApiResponse.success(postRevisionRepository.findRevisionSummariesByPostId(postId));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostRevisionDetailResponse> getPostRevision(Long postId, Long revisionId) {
		PostRevision revision = findRevisionForPost(postId, revisionId);
		return ApiResponse.success(toDetailResponse(revision));
	}

	@Override
	@Transactional
	public ApiResponse<PostResponse> revertToRevision(Long postId, Long revisionId, Integer expectedRevisionNumber) {
		Post post = findPostForUpdateOrThrow(postId);
		assertExpectedRevision(postId, expectedRevisionNumber);
		PostRevision revision = findRevisionForPost(postId, revisionId);
		PostRevisionSnapshot snapshot = snapshotFor(revision);
		String previousSlug = post.getSlug();
		String previousPath = post.getPath();

		post.setTitle(snapshot.title());
		post.setSummary(snapshot.summary());
		post.setContent(snapshot.content());
		post.setContentType(snapshot.contentType());
		if (revision.getSnapshotJson() != null) {
			restoreEditableMetadata(post, snapshot);
		}
		refreshContentMetadata(post);
		postRepository.save(post);
		if (previousPath != null && !previousPath.equals(post.getPath())) {
			postRepository.replaceDescendantPathPrefix(post.getId(), previousPath, post.getPath());
		}

		persistRevision(post, PostRevisionKind.RESTORED, "Restored from version " + revision.getVersionNumber(),
				revision.getId());
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.UPDATED, previousSlug));
		log.info("Reverted post {} to revision {}", postId, revision.getVersionNumber());

		return ApiResponse.success("Post restored from revision " + revision.getVersionNumber(),
				postMapper.toResponse(post));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostDiffResponse> compareRevisions(Long postId, Long baseRevisionId, Long targetRevisionId) {
		PostRevision base = findRevisionForPost(postId, baseRevisionId);
		PostRevision target = findRevisionForPost(postId, targetRevisionId);

		return ApiResponse.success(new PostDiffResponse(createFieldDiff(base.getTitle(), target.getTitle()),
				createFieldDiff(base.getSummary(), target.getSummary()),
				createFieldDiff(base.getContent(), target.getContent())));
	}

	private PostRevision persistRevision(Post lockedPost, PostRevisionKind revisionKind, String changeSummary,
			Long sourceRevisionId) {
		PostRevisionSnapshot snapshot = toSnapshot(lockedPost);
		String snapshotJson = serializeSnapshot(snapshot);
		String snapshotHash = sha256(snapshotJson);
		Optional<PostRevision> latestRevision = postRevisionRepository
				.findFirstByPostIdOrderByVersionNumberDesc(lockedPost.getId());

		if (revisionKind == PostRevisionKind.UPDATED && latestRevision
				.map(revision -> Objects.equals(snapshotHash, revision.getSnapshotHash())).orElse(false)) {
			log.debug("Skipped duplicate revision for post {}", lockedPost.getId());
			return latestRevision.orElseThrow();
		}

		PostRevision revision = new PostRevision();
		revision.setPost(lockedPost);
		revision.setParentRevision(latestRevision.orElse(null));
		revision.setTitle(lockedPost.getTitle());
		revision.setSummary(lockedPost.getSummary());
		revision.setContent(lockedPost.getContent());
		revision.setContentType(lockedPost.getContentType());
		revision.setVersionNumber(latestRevision.map(value -> value.getVersionNumber() + 1).orElse(1));
		revision.setBaseVersionNumber(latestRevision.map(PostRevision::getVersionNumber).orElse(0));
		revision.setSourceRevisionId(sourceRevisionId);
		revision.setRevisionKind(revisionKind);
		revision.setChangeType(revisionKind.name());
		revision.setChangeSummary(changeSummary);
		revision.setContentHash(lockedPost.getContentHash());
		revision.setSnapshotJson(snapshotJson);
		revision.setSnapshotHash(snapshotHash);
		revision.setCreatedBy(SecurityUtil.getCurrentUserOrThrow(userRepository));

		PostRevision savedRevision = postRevisionRepository.save(revision);
		log.info("Saved post revision {} for post {} ({})", savedRevision.getVersionNumber(), lockedPost.getId(),
				revisionKind);
		return savedRevision;
	}

	private PostRevisionSnapshot toSnapshot(Post post) {
		Set<Long> tagIds = Optional.ofNullable(post.getTags()).orElseGet(Set::of).stream().map(tag -> tag.getId())
				.filter(Objects::nonNull)
				.collect(java.util.stream.Collectors.toCollection(TreeSet::new));
		return new PostRevisionSnapshot(post.getTitle(), post.getSlug(), post.getCoverImage(), post.getSummary(),
				post.getContent(), post.getContentType(), post.getStatus(), post.getIsFeatured(),
				post.getCategory() == null ? null : post.getCategory().getId(), tagIds,
				post.getSeries() == null ? null : post.getSeries().getId(), post.getSeriesOrder(),
				post.getParent() == null ? null : post.getParent().getId());
	}

	private PostRevisionSnapshot snapshotFor(PostRevision revision) {
		if (revision.getSnapshotJson() == null) {
			return new PostRevisionSnapshot(revision.getTitle(), null, null, revision.getSummary(), revision.getContent(),
					revision.getContentType(), null, false, null, Set.of(), null, null, null);
		}
		try {
			return objectMapper.readValue(revision.getSnapshotJson(), PostRevisionSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new BusinessException(500, "Stored post revision " + revision.getId() + " is invalid");
		}
	}

	/**
	 * Restores the author-controlled fields captured by a modern revision. Workflow,
	 * review, archive, and interaction fields deliberately remain untouched.
	 */
	private void restoreEditableMetadata(Post post, PostRevisionSnapshot snapshot) {
		String snapshotSlug = snapshot.slug();
		Assert.notBlank(snapshotSlug,
				() -> new BusinessException(500, "Stored post revision is missing its slug"));
		postRepository.findBySlug(snapshotSlug).filter(candidate -> !candidate.getId().equals(post.getId()))
				.ifPresent(candidate -> {
					throw new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Cannot restore revision because slug is already in use: " + snapshotSlug);
				});

		post.setSlug(snapshotSlug);
		post.setCoverImage(snapshot.coverImage());
		post.setIsFeatured(Boolean.TRUE.equals(snapshot.featured()));
		restoreCategory(post, snapshot.categoryId());
		restoreTags(post, snapshot.tagIds());
		restoreSeries(post, snapshot.seriesId(), snapshot.seriesOrder());
		restoreParent(post, snapshot.parentId());
	}

	private void restoreCategory(Post post, Long categoryId) {
		if (categoryId == null) {
			post.setCategory(null);
			return;
		}
		post.setCategory(categoryRepository.findById(categoryId).orElseThrow(
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Cannot restore revision because category " + categoryId + " no longer exists")));
	}

	private void restoreTags(Post post, Set<Long> tagIds) {
		Set<Long> snapshotTagIds = tagIds == null ? Set.of() : tagIds;
		if (snapshotTagIds.isEmpty()) {
			post.setTags(new HashSet<>());
			return;
		}

		var tags = tagRepository.findAllById(snapshotTagIds);
		Assert.isTrue(tags.size() == snapshotTagIds.size(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Cannot restore revision because one or more tags no longer exist"));
		post.setTags(new HashSet<>(tags));
	}

	private void restoreSeries(Post post, Long seriesId, Integer seriesOrder) {
		if (seriesId == null) {
			post.setSeries(null);
			post.setSeriesOrder(0);
			return;
		}
		post.setSeries(seriesRepository.findById(seriesId).orElseThrow(
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Cannot restore revision because series " + seriesId + " no longer exists")));
		post.setSeriesOrder(seriesOrder == null ? 0 : seriesOrder);
	}

	private void restoreParent(Post post, Long parentId) {
		if (parentId == null) {
			post.setParent(null);
			post.updatePath(null);
			return;
		}
		Assert.isFalse(parentId.equals(post.getId()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A post cannot be its own parent"));
		Post parent = postRepository.findById(parentId).orElseThrow(() -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Cannot restore revision because parent post " + parentId + " no longer exists"));
		Assert.notBlank(parent.getPath(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Cannot restore revision because parent post has no hierarchy path"));
		Assert.isFalse(parent.getPath().contains("/" + post.getId() + "/"), () -> new BusinessException(
				BusinessCode.BAD_REQUEST, "A post cannot be moved below one of its descendants"));
		post.setParent(parent);
		post.updatePath(parent);
	}

	private PostRevisionDetailResponse toDetailResponse(PostRevision revision) {
		User createdBy = revision.getCreatedBy();
		return new PostRevisionDetailResponse(revision.getId(), revision.getPost().getId(), revision.getVersionNumber(),
				revision.getRevisionKind(), revision.getChangeType(), revision.getChangeSummary(),
				revision.getBaseVersionNumber(),
				revision.getParentRevision() == null ? null : revision.getParentRevision().getId(),
				revision.getSourceRevisionId(), revision.getContentHash(), revision.getSnapshotHash(),
				createdBy == null ? null : createdBy.getUsername(), revision.getCreatedAt(), snapshotFor(revision));
	}

	private Post findPostForUpdateOrThrow(Long postId) {
		return postRepository.findByIdForUpdate(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
	}

	private PostRevision findRevisionForPost(Long postId, Long revisionId) {
		PostRevision revision = postRevisionRepository.findById(revisionId)
				.orElseThrow(() -> new ResourceNotFoundException("PostRevision", "id", revisionId));
		Assert.isTrue(revision.getPost().getId().equals(postId),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Revision does not belong to this post"));
		return revision;
	}

	private void assertPostExists(Long postId) {
		Assert.isTrue(postRepository.existsById(postId), () -> new ResourceNotFoundException("Post", "id", postId));
	}

	private String serializeSnapshot(PostRevisionSnapshot snapshot) {
		try {
			return objectMapper.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new BusinessException(500, "Unable to serialize post revision");
		}
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private PostDiffResponse.FieldDiff createFieldDiff(String original, String revised) {
		String orgVal = original != null ? original : "";
		String revVal = revised != null ? revised : "";
		boolean changed = !Objects.equals(orgVal, revVal);

		String diffHtml = "";
		if (changed) {
			List<String> originalLines = Arrays.asList(orgVal.split("\n"));
			List<String> revisedLines = Arrays.asList(revVal.split("\n"));
			Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);

			StringBuilder html = new StringBuilder("<div class=\"diff-container\">");
			int currentLine = 0;
			for (AbstractDelta<String> delta : patch.getDeltas()) {
				while (currentLine < delta.getSource().getPosition()) {
					html.append("<div class=\"line-unchanged\">").append(escapeHtml(originalLines.get(currentLine)))
							.append("</div>");
					currentLine++;
				}
				for (String line : delta.getSource().getLines()) {
					html.append("<div class=\"line-deleted\">- ").append(escapeHtml(line)).append("</div>");
					currentLine++;
				}
				for (String line : delta.getTarget().getLines()) {
					html.append("<div class=\"line-inserted\">+ ").append(escapeHtml(line)).append("</div>");
				}
			}
			while (currentLine < originalLines.size()) {
				html.append("<div class=\"line-unchanged\">").append(escapeHtml(originalLines.get(currentLine)))
						.append("</div>");
				currentLine++;
			}
			html.append("</div>");
			diffHtml = html.toString();
		}

		return new PostDiffResponse.FieldDiff(orgVal, revVal, changed, diffHtml);
	}

	private String escapeHtml(String input) {
		return input == null ? ""
				: input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
						.replace("'", "&#39;");
	}

	private void refreshContentMetadata(Post post) {
		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze(post.getTitle(), post.getSummary(),
				post.getContent(), post.getContentType());
		post.setWordCount(metadata.wordCount());
		post.setReadingTimeMinutes(metadata.readingTimeMinutes());
		post.setAutoSummary(metadata.autoSummary());
		post.setToc(metadata.toc());
		post.setContentHash(metadata.contentHash());
	}
}
