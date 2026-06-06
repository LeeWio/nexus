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
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.search.type", havingValue = "database")
public class DatabasePostSearchServiceImpl extends AbstractPostSearchService
{

	public DatabasePostSearchServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
			TagRepository tagRepository, ProjectRepository projectRepository, MomentRepository momentRepository)
	{
		super(postRepository, categoryRepository, tagRepository, projectRepository, momentRepository);
	}

	@Override
	public void indexPost(Post post)
	{
		log.debug("Database search mode enabled, skipping indexing for post: {}", post.getId());
	}

	@Override
	public void deletePostIndex(Long postId)
	{
		log.debug("Database search mode enabled, skipping index deletion for post: {}", postId);
	}

	@Override
	public void rebuildIndex()
	{
		log.debug("Database search mode enabled, skipping index rebuild.");
	}

	@Override
	public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable)
	{
		Specification<Post> spec = (root, query, cb) ->
		{
			Specification<Post> statusSpec = (r, q, c) -> c.equal(r.get("status"), PostStatus.PUBLISHED);

			if (StrUtil.isBlank(keyword))
			{
				return statusSpec.toPredicate(root, query, cb);
			}

			String pattern = "%" + keyword.toLowerCase() + "%";
			Specification<Post> keywordSpec = (r, q, c) -> c.or(c.like(r.get("title"), pattern),
					c.like(r.get("summary"), pattern), c.like(r.get("content"), pattern));

			return Specification.where(statusSpec).and(keywordSpec).toPredicate(root, query, cb);
		};

		Page<Post> posts = postRepository.findAll(spec, pageable);
		Page<PostDocument> documents = posts.map(this::mapToDocument);

		return ApiResponse.success(PageResult.of(documents));
	}

	@Override
	public ApiResponse<List<String>> getSearchSuggestions(String keyword)
	{
		if (StrUtil.isBlank(keyword))
		{
			return ApiResponse.success(List.of());
		}
		List<String> suggestions = postRepository
				.findTop5ByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED).stream()
				.map(Post::getTitle).toList();
		return ApiResponse.success(suggestions);
	}

	@Override
	protected List<QuickSearchResponse.SearchResultItem> searchQuickPosts(String keyword)
	{
		return postRepository.findTop5ByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED).stream()
				.map(p -> new QuickSearchResponse.SearchResultItem(p.getId().toString(), p.getTitle(),
						"/posts/" + p.getSlug()))
				.toList();
	}

	@Override
	protected void searchPostsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups)
	{
		Specification<Post> postSpec = (root, query, cb) ->
		{
			String pattern = "%" + keyword.toLowerCase() + "%";
			return cb.and(cb.equal(root.get("status"), PostStatus.PUBLISHED), cb.or(
					cb.like(cb.lower(root.get("title")), pattern), cb.like(cb.lower(root.get("summary")), pattern)));
		};

		List<UnifiedSearchResponse.SearchResultItem> items = postRepository
				.findAll(postSpec, org.springframework.data.domain.PageRequest.of(0, 5)).stream()
				.map(p -> UnifiedSearchResponse.SearchResultItem.builder().id("post:" + p.getId()).title(p.getTitle())
						.subtitle(formatSubtitle(p)).description(p.getSummary()).url("/post/" + p.getSlug())
						.icon("book-text").iconColor("#3b82f6").type("POST").build())
				.collect(Collectors.toList());

		if (!items.isEmpty())
		{
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("POST").label("Articles").priority(10)
					.items(items).build());
		}
	}
}
