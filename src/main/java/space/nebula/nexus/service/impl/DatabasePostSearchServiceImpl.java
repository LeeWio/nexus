package space.nebula.nexus.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.QuickSearchResponse;
import space.nebula.nexus.payload.response.UnifiedSearchResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.search.type", havingValue = "database")
public class DatabasePostSearchServiceImpl extends AbstractPostSearchService {

	public DatabasePostSearchServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
			TagRepository tagRepository, ProjectRepository projectRepository, MomentRepository momentRepository) {
		super(postRepository, categoryRepository, tagRepository, projectRepository, momentRepository);
	}

	@Override
	public void indexPost(Post post) {
		log.debug("Database search mode enabled, skipping indexing for post: {}", post.getId());
	}

	@Override
	public void deletePostIndex(Long postId) {
		log.debug("Database search mode enabled, skipping index deletion for post: {}", postId);
	}

	@Override
	public void rebuildIndex() {
		log.debug("Database search mode enabled, skipping index rebuild.");
	}

	@Override
	public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable) {
		Specification<Post> spec = (root, query, cb) -> {
			Specification<Post> statusSpec = (r, q, c) -> c.equal(r.get("status"), PostStatus.PUBLISHED);

			if (StrUtil.isBlank(keyword)) {
				return statusSpec.toPredicate(root, query, cb);
			}

			String normalizedPattern = normalizedLikePattern(keyword);
			String rawPattern = rawLikePattern(keyword);
			Specification<Post> keywordSpec = (r, q, c) -> c.or(c.like(c.lower(r.get("title")), normalizedPattern),
					c.like(c.lower(r.get("summary")), normalizedPattern), c.like(r.get("content"), rawPattern));

			return Specification.where(statusSpec).and(keywordSpec).toPredicate(root, query, cb);
		};

		Page<Post> posts = postRepository.findAll(spec, pageable);
		Page<PostDocument> documents = posts.map(p -> highlightDocument(mapToDocument(p), keyword));

		return ApiResponse.success(PageResult.of(documents));
	}

	@Override
	public ApiResponse<List<String>> getSearchSuggestions(String keyword) {
		if (StrUtil.isBlank(keyword)) {
			return ApiResponse.success(List.of());
		}
		List<String> suggestions = postRepository
				.findTop5ByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED).stream()
				.map(Post::getTitle).toList();
		return ApiResponse.success(suggestions);
	}

	@Override
	protected List<QuickSearchResponse.SearchResultItem> searchQuickPosts(String keyword) {
		return postRepository.findTop5ByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED).stream()
				.map(p -> new QuickSearchResponse.SearchResultItem(p.getId().toString(), p.getTitle(),
						"/posts/" + p.getSlug()))
				.toList();
	}

	@Override
	protected void searchPostsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		Specification<Post> postSpec = (root, query, cb) -> {
			String normalizedPattern = normalizedLikePattern(keyword);
			String rawPattern = rawLikePattern(keyword);
			return cb.and(cb.equal(root.get("status"), PostStatus.PUBLISHED),
					cb.or(cb.like(cb.lower(root.get("title")), normalizedPattern),
							cb.like(cb.lower(root.get("summary")), normalizedPattern),
							cb.like(root.get("content"), rawPattern)));
		};

		List<UnifiedSearchResponse.SearchResultItem> items = postRepository
				.findAll(postSpec, org.springframework.data.domain.PageRequest.of(0, 5)).stream()
				.map(this::mapToDocument).map(doc -> highlightDocument(doc, keyword))
				.map(p -> UnifiedSearchResponse.SearchResultItem.builder().id("post:" + p.getId()).title(p.getTitle())
						.subtitle(p.getPublishedAt() != null ? p.getPublishedAt().toLocalDate().toString() : "")
						.description(p.getSummary()).url("/post/" + p.getSlug()).icon("book-text").iconColor("#3b82f6")
						.type("POST").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("POST").label("Articles").priority(10)
					.items(items).build());
		}
	}

	/**
	 * Local Java-based Regex highlighter that emulates Elasticsearch highlighting.
	 * Preserves the original casing of the matched characters and extracts content
	 * fallback windows.
	 */
	private PostDocument highlightDocument(PostDocument doc, String keyword) {
		if (StrUtil.isBlank(keyword)) {
			return doc;
		}

		String escapedKeyword = java.util.regex.Pattern.quote(keyword);
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)(" + escapedKeyword + ")");

		// 1. Highlight Title
		if (StrUtil.isNotBlank(doc.getTitle())) {
			java.util.regex.Matcher m = pattern.matcher(doc.getTitle());
			if (m.find()) {
				doc.setTitle(m.replaceAll("<mark class=\"search-highlight\">$1</mark>"));
			}
		}

		// 2. Highlight Summary & fallback to Content snippet
		boolean summaryHasKeyword = false;
		if (StrUtil.isNotBlank(doc.getSummary())) {
			java.util.regex.Matcher m = pattern.matcher(doc.getSummary());
			if (m.find()) {
				doc.setSummary(m.replaceAll("<mark class=\"search-highlight\">$1</mark>"));
				summaryHasKeyword = true;
			}
		}

		if (!summaryHasKeyword && StrUtil.isNotBlank(doc.getContent())) {
			java.util.regex.Matcher m = pattern.matcher(doc.getContent());
			if (m.find()) {
				int matchIndex = m.start();
				int start = Math.max(0, matchIndex - 45);
				int end = Math.min(doc.getContent().length(), matchIndex + keyword.length() + 45);

				String snippet = doc.getContent().substring(start, end);
				String prefix = start > 0 ? "... " : "";
				String suffix = end < doc.getContent().length() ? " ..." : "";

				java.util.regex.Matcher snippetMatcher = pattern.matcher(snippet);
				String highlightedSnippet = snippetMatcher.replaceAll("<mark class=\"search-highlight\">$1</mark>");

				doc.setSummary(prefix + highlightedSnippet + suffix);
			}
		}

		return doc;
	}

	private String normalizedLikePattern(String keyword) {
		return "%" + keyword.toLowerCase(Locale.ROOT) + "%";
	}

	private String rawLikePattern(String keyword) {
		return "%" + keyword + "%";
	}
}
