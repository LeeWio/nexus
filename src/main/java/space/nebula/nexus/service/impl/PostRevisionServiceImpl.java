package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.PostRevisionMapper;
import space.nebula.nexus.payload.response.PostDiffResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostRevisionRepository;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostSearchService;
import space.nebula.nexus.utils.PostContentAnalyzer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostRevisionServiceImpl implements IPostRevisionService {

	private final PostRevisionRepository postRevisionRepository;
	private final PostRevisionMapper postRevisionMapper;
	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final IPostSearchService postSearchService;

	@Override
	@Transactional
	public void saveRevision(Post post) {
		saveRevision(post, "SNAPSHOT", "Post snapshot saved");
	}

	@Override
	@Transactional
	public void saveRevision(Post post, String changeType, String changeSummary) {
		Post lockedPost = postRepository.findByIdForUpdate(post.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", post.getId()));
		int nextVersion = postRevisionRepository.findMaxVersionByPostId(post.getId()).orElse(0) + 1;

		PostRevision revision = new PostRevision();
		revision.setPost(post);
		revision.setTitle(post.getTitle());
		revision.setSummary(post.getSummary());
		revision.setContent(post.getContent());
		revision.setContentType(post.getContentType());
		revision.setVersionNumber(nextVersion);
		revision.setChangeType(changeType);
		revision.setChangeSummary(changeSummary);
		revision.setContentHash(post.getContentHash());
		revision.setCreatedBy(lockedPost.getAuthor());

		postRevisionRepository.save(revision);
		log.info("Saved revision {} for post {}", nextVersion, post.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<PostRevisionResponse>> getPostRevisions(Long postId) {
		Assert.isTrue(postRepository.existsById(postId), () -> new BusinessException(404, "Post not found"));
		List<PostRevision> revisions = postRevisionRepository.findByPostIdOrderByVersionNumberDesc(postId);
		return ApiResponse.success(postRevisionMapper.toResponseList(revisions));
	}

	@Override
	@Transactional
	public ApiResponse<PostResponse> revertToRevision(Long postId, Long revisionId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

		PostRevision revision = postRevisionRepository.findById(revisionId)
				.orElseThrow(() -> new ResourceNotFoundException("PostRevision", "id", revisionId));

		Assert.isTrue(revision.getPost().getId().equals(postId),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Revision does not belong to this post"));

		// Revert fields
		post.setTitle(revision.getTitle());
		post.setSummary(revision.getSummary());
		post.setContent(revision.getContent());
		post.setContentType(revision.getContentType());
		refreshContentMetadata(post);

		// Save post
		postRepository.save(post);

		// Save a new revision representing the restoration
		saveRevision(post);

		// Update Search Index
		postSearchService.indexPost(post);

		log.info("Reverted post {} to revision {}", postId, revisionId);

		return ApiResponse.success("Post reverted to revision " + revision.getVersionNumber(),
				postMapper.toResponse(post));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostDiffResponse> compareRevisions(Long postId, Long baseRevisionId, Long targetRevisionId) {
		PostRevision base = postRevisionRepository.findById(baseRevisionId)
				.orElseThrow(() -> new ResourceNotFoundException("PostRevision", "id", baseRevisionId));
		PostRevision target = postRevisionRepository.findById(targetRevisionId)
				.orElseThrow(() -> new ResourceNotFoundException("PostRevision", "id", targetRevisionId));

		Assert.isTrue(base.getPost().getId().equals(postId) && target.getPost().getId().equals(postId),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Revisions do not belong to the specified post"));

		return ApiResponse.success(new PostDiffResponse(createFieldDiff(base.getTitle(), target.getTitle()),
				createFieldDiff(base.getSummary(), target.getSummary()),
				createFieldDiff(base.getContent(), target.getContent())));
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
				// Add unchanged lines before delta
				while (currentLine < delta.getSource().getPosition()) {
					html.append("<div class=\"line-unchanged\">").append(escapeHtml(originalLines.get(currentLine)))
							.append("</div>");
					currentLine++;
				}

				// Add deletions
				for (String line : delta.getSource().getLines()) {
					html.append("<div class=\"line-deleted\">- ").append(escapeHtml(line)).append("</div>");
					currentLine++;
				}

				// Add insertions
				for (String line : delta.getTarget().getLines()) {
					html.append("<div class=\"line-inserted\">+ ").append(escapeHtml(line)).append("</div>");
				}
			}

			// Add remaining unchanged lines
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
		if (input == null)
			return "";
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
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
