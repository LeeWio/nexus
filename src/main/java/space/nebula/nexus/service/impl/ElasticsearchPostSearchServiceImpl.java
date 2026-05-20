package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.QuickSearchResponse;
import space.nebula.nexus.payload.response.UnifiedSearchResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IPostSearchService;
import space.nebula.nexus.enums.PostStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.type", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchPostSearchServiceImpl implements IPostSearchService {

	private final PostSearchRepository postSearchRepository;
	private final PostRepository postRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final ProjectRepository projectRepository;
	private final MomentRepository momentRepository;

	@Async("asyncExecutor")
	@Override
	public void indexPost(Post post) {
		if (post.getStatus() != PostStatus.PUBLISHED) {
			deletePostIndex(post.getId());
			return;
		}
		try {
			PostDocument document = mapToDocument(post);
			postSearchRepository.save(document);
			log.info("Successfully indexed post to Elasticsearch: {}", post.getId());
		} catch (Exception e) {
			log.error("Failed to index post to Elasticsearch: {}", post.getId(), e);
		}
	}

	@Async("asyncExecutor")
	@Override
	public void deletePostIndex(Long postId) {
		try {
			postSearchRepository.deleteById(postId.toString());
			log.info("Successfully deleted post from Elasticsearch: {}", postId);
		} catch (Exception e) {
			log.error("Failed to delete post from Elasticsearch: {}", postId, e);
		}
	}

	@Async("asyncExecutor")
	@Override
	public void rebuildIndex() {
		log.info("Starting Elasticsearch index rebuild for posts...");
		postSearchRepository.deleteAll();

		int page = 0;
		int size = 100;
		long totalIndexed = 0;

		org.springframework.data.domain.Page<Post> postPage;
		do {
			postPage = postRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
			List<PostDocument> documents = postPage.getContent().stream()
					.filter(p -> p.getStatus() == PostStatus.PUBLISHED).map(this::mapToDocument).toList();

			if (!documents.isEmpty()) {
				postSearchRepository.saveAll(documents);
				totalIndexed += documents.size();
			}
			page++;
		} while (postPage.hasNext());

		log.info("Finished rebuilding Elasticsearch index. Total posts indexed: {}", totalIndexed);
	}

	@Override
	public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable) {
		Page<PostDocument> page;
		if (keyword == null || keyword.isBlank()) {
			page = postSearchRepository.findAll(pageable);
		} else {
			page = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword, pageable);
		}
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	public ApiResponse<QuickSearchResponse> quickSearch(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return ApiResponse.success(new QuickSearchResponse(List.of(), List.of(), List.of()));
		}

		// 1. Search Posts in ES (Limit 5)
		var postPage = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword,
				org.springframework.data.domain.PageRequest.of(0, 5));
		var posts = postPage.getContent().stream()
				.map(p -> new QuickSearchResponse.SearchResultItem(p.getId(), p.getTitle(), "/posts/" + p.getSlug()))
				.toList();

		// 2. Search Categories in JPA (Limit 3)
		var categories = categoryRepository.findByNameContainingIgnoreCase(keyword).stream().limit(3)
				.map(c -> new QuickSearchResponse.SearchResultItem(c.getId().toString(), c.getName(),
						"/categories/" + c.getSlug()))
				.toList();

		// 3. Search Tags in JPA (Limit 3)
		var tags = tagRepository.findByNameContainingIgnoreCase(keyword).stream().limit(3)
				.map(t -> new QuickSearchResponse.SearchResultItem(t.getId().toString(), t.getName(),
						"/tags/" + t.getSlug()))
				.toList();

		return ApiResponse.success(new QuickSearchResponse(posts, categories, tags));
	}

	@Override
	public ApiResponse<UnifiedSearchResponse> unifiedSearch(String keyword) {
		long startTime = System.currentTimeMillis();
		List<UnifiedSearchResponse.SearchGroup> groups = new ArrayList<>();

		if (keyword == null || keyword.isBlank()) {
			addRecentPostsGroup(groups);
			groups.add(createActionGroup());
			return ApiResponse.success(UnifiedSearchResponse.builder().groups(groups).totalHits(0)
					.processingTimeMs(System.currentTimeMillis() - startTime).build());
		}

		// 1. Posts in ES
		searchPostsProfessionalES(keyword, groups);

		// Fallback to JPA for other entities (consistent with Database service)
		searchProjectsProfessional(keyword, groups);
		searchCategoriesProfessional(keyword, groups);
		searchTagsProfessional(keyword, groups);
		searchMomentsProfessional(keyword, groups);

		// Sort groups by priority
		groups.sort(java.util.Comparator.comparingInt(UnifiedSearchResponse.SearchGroup::getPriority));

		long totalHits = groups.stream().mapToLong(g -> g.getItems().size()).sum();

		return ApiResponse.success(UnifiedSearchResponse.builder().groups(groups).totalHits(totalHits)
				.processingTimeMs(System.currentTimeMillis() - startTime).build());
	}

	private void addRecentPostsGroup(List<UnifiedSearchResponse.SearchGroup> groups) {
		var recentPosts = postRepository.findAllByStatus(PostStatus.PUBLISHED,
				org.springframework.data.domain.PageRequest.of(0, 3, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "publishedAt")));

		List<UnifiedSearchResponse.SearchResultItem> items = recentPosts.getContent().stream()
				.map(p -> UnifiedSearchResponse.SearchResultItem.builder().id("post:" + p.getId()).title(p.getTitle())
						.subtitle(formatSubtitle(p)).url("/post/" + p.getSlug()).icon("clock").iconColor("#3b82f6")
						.type("POST").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("POST").label("Recent Articles").priority(1)
					.items(items).build());
		}
	}

	private void searchPostsProfessionalES(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		// ES automatically handles weighting if configured in repository, 
		// otherwise we use multi-match with boosting here if we were using RestClient directly.
		// For simplicity with Spring Data ES repository:
		var postPage = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword,
				org.springframework.data.domain.PageRequest.of(0, 5));

		List<UnifiedSearchResponse.SearchResultItem> items = postPage.getContent().stream()
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

	private void searchProjectsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		List<UnifiedSearchResponse.SearchResultItem> items = projectRepository
				.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsPublishedTrue(keyword, keyword)
				.stream().limit(3)
				.map(p -> UnifiedSearchResponse.SearchResultItem.builder().id("project:" + p.getId()).title(p.getName())
						.subtitle("Personal Project").description(p.getDescription()).url("/project/" + p.getSlug())
						.icon("layout").iconColor("#8b5cf6").type("PROJECT").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("PROJECT").label("Projects").priority(20)
					.items(items).build());
		}
	}

	private void searchCategoriesProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		List<UnifiedSearchResponse.SearchResultItem> items = categoryRepository.findByNameContainingIgnoreCase(keyword)
				.stream().limit(3)
				.map(c -> UnifiedSearchResponse.SearchResultItem.builder().id("category:" + c.getId())
						.title(c.getName()).subtitle("Category").url("/category/" + c.getSlug()).icon("folder")
						.iconColor("#10b981").type("CATEGORY").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("CATEGORY").label("Categories").priority(30)
					.items(items).build());
		}
	}

	private void searchTagsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		List<UnifiedSearchResponse.SearchResultItem> items = tagRepository.findByNameContainingIgnoreCase(keyword)
				.stream().limit(3)
				.map(t -> UnifiedSearchResponse.SearchResultItem.builder().id("tag:" + t.getId()).title(t.getName())
						.subtitle("Tag").url("/tag/" + t.getSlug()).icon("tag").iconColor("#f59e0b").type("TAG").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("TAG").label("Tags").priority(40).items(items)
					.build());
		}
	}

	private void searchMomentsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
		List<UnifiedSearchResponse.SearchResultItem> items = momentRepository
				.findByContentContainingIgnoreCaseAndIsPublishedTrue(keyword).stream().limit(3)
				.map(m -> UnifiedSearchResponse.SearchResultItem.builder().id("moment:" + m.getId())
						.title(truncateContent(m.getContent())).subtitle("Moment").url("/moment/" + m.getId())
						.icon("message-square").iconColor("#6366f1").type("MOMENT").build())
				.collect(Collectors.toList());

		if (!items.isEmpty()) {
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("MOMENT").label("Moments").priority(50)
					.items(items).build());
		}
	}

	private UnifiedSearchResponse.SearchGroup createActionGroup() {
		List<UnifiedSearchResponse.SearchResultItem> actions = List.of(
				UnifiedSearchResponse.SearchResultItem.builder().id("action:home").title("Go to Home")
						.subtitle("Navigation").url("/").icon("home").iconColor("#64748b").type("ACTION")
						.shortcut(List.of("G", "H")).build(),
				UnifiedSearchResponse.SearchResultItem.builder().id("action:guestbook").title("Sign Guestbook")
						.subtitle("Interaction").url("/guestbook").icon("pen-tool").iconColor("#64748b").type("ACTION")
						.shortcut(List.of("G", "B")).build());

		return UnifiedSearchResponse.SearchGroup.builder().type("ACTION").label("Quick Actions").priority(100)
				.items(actions).build();
	}

	private String formatSubtitle(Post p) {
		String date = p.getPublishedAt() != null ? p.getPublishedAt().toLocalDate().toString() : "Draft";
		return String.format("%s • %d views", date, p.getViews());
	}

	private String truncateContent(String content) {
		if (content == null)
			return "";
		return content.length() > 50 ? content.substring(0, 47) + "..." : content;
	}

	private PostDocument mapToDocument(Post post) {
		List<String> tagNames = post.getTags() != null
				? post.getTags().stream().map(Tag::getName).collect(Collectors.toList())
				: List.of();

		return PostDocument.builder().id(post.getId().toString()).title(post.getTitle()).slug(post.getSlug())
				.summary(post.getSummary()).content(post.getContent())
				.authorName(post.getAuthor() != null ? post.getAuthor().getNickname() : null)
				.categoryName(post.getCategory() != null ? post.getCategory().getName() : null).tags(tagNames)
				.publishedAt(post.getPublishedAt()).views(post.getViews()).build();
	}
}
