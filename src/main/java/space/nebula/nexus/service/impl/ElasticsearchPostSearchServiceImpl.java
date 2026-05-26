package space.nebula.nexus.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
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
import space.nebula.nexus.enums.PostStatus;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.search.type", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchPostSearchServiceImpl extends AbstractPostSearchService {

	private final PostSearchRepository postSearchRepository;

	public ElasticsearchPostSearchServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
			TagRepository tagRepository, ProjectRepository projectRepository, MomentRepository momentRepository,
			PostSearchRepository postSearchRepository) {
		super(postRepository, categoryRepository, tagRepository, projectRepository, momentRepository);
		this.postSearchRepository = postSearchRepository;
	}

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
		if (StrUtil.isBlank(keyword)) {
			page = postSearchRepository.findAll(pageable);
		} else {
			page = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword, pageable);
		}
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	protected List<QuickSearchResponse.SearchResultItem> searchQuickPosts(String keyword) {
		var postPage = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword,
				org.springframework.data.domain.PageRequest.of(0, 5));
		return postPage.getContent().stream()
				.map(p -> new QuickSearchResponse.SearchResultItem(p.getId(), p.getTitle(), "/posts/" + p.getSlug()))
				.toList();
	}

	@Override
	protected void searchPostsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups) {
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
}
