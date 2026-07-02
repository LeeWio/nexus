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
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.search.type", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchPostSearchServiceImpl extends AbstractPostSearchService
{

	private final PostSearchRepository postSearchRepository;
	private final ElasticsearchOperations elasticsearchOperations;

	public ElasticsearchPostSearchServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
			TagRepository tagRepository, ProjectRepository projectRepository, MomentRepository momentRepository,
			PostSearchRepository postSearchRepository, ElasticsearchOperations elasticsearchOperations)
	{
		super(postRepository, categoryRepository, tagRepository, projectRepository, momentRepository);
		this.postSearchRepository = postSearchRepository;
		this.elasticsearchOperations = elasticsearchOperations;
	}

	@Async("asyncExecutor")
	@Override
	public void indexPost(Post post)
	{
		if (post.getStatus() != PostStatus.PUBLISHED)
		{
			deletePostIndex(post.getId());
			return;
		}
		try
		{
			PostDocument document = mapToDocument(post);
			postSearchRepository.save(document);
			log.info("Successfully indexed post to Elasticsearch: {}", post.getId());
		}
		catch (Exception e)
		{
			log.error("Failed to index post to Elasticsearch: {}", post.getId(), e);
		}
	}

	@Async("asyncExecutor")
	@Override
	public void deletePostIndex(Long postId)
	{
		try
		{
			postSearchRepository.deleteById(postId.toString());
			log.info("Successfully deleted post from Elasticsearch: postId={}", postId);
		}
		catch (Exception e) {
			log.error("Failed to delete post from Elasticsearch: postId={}", postId, e);
		}
	}

	@Async("asyncExecutor")
	@Override
	public void rebuildIndex()
	{
		log.info("Starting Blue-Green Elasticsearch index rebuild for posts...");
		
		String aliasName = "nexus-post";
		String newIndexName = aliasName + "-" + System.currentTimeMillis();
		
		// 1. Create new index with mappings
		IndexOperations newIndexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(newIndexName));
		newIndexOps.create();
		newIndexOps.putMapping(newIndexOps.createMapping(PostDocument.class));

		int page = 0;
		int size = 100;
		long totalIndexed = 0;

		org.springframework.data.domain.Page<Post> postPage;
		do
		{
			postPage = postRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
			List<PostDocument> documents = postPage.getContent().stream()
					.filter(p -> p.getStatus() == PostStatus.PUBLISHED).map(this::mapToDocument).toList();

			if (!documents.isEmpty())
			{
				elasticsearchOperations.save(documents, IndexCoordinates.of(newIndexName));
				totalIndexed += documents.size();
			}
			page++;
		}
		while (postPage.hasNext());

		// 2. Atomic Alias Switch
		IndexOperations indexOps = elasticsearchOperations.indexOps(PostDocument.class);
		AliasActions aliasActions = new AliasActions();
		
		// Remove alias from all old indices
		try {
			Map<String, Set<org.springframework.data.elasticsearch.core.index.AliasData>> aliases = indexOps.getAliases();
			aliases.keySet().forEach(oldIndex -> {
				aliasActions.add(new AliasAction.Remove(AliasActionParameters.builder()
						.withIndices(oldIndex)
						.withAliases(aliasName)
						.build()));
			});
		} catch (Exception e) {
			log.debug("No existing alias found or error fetching aliases: {}", e.getMessage());
		}

		// Add alias to new index
		aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
				.withIndices(newIndexName)
				.withAliases(aliasName)
				.build()));
		
		indexOps.alias(aliasActions);

		// 3. Cleanup old indices (Optional but recommended)
		try {
			Map<String, Set<org.springframework.data.elasticsearch.core.index.AliasData>> oldIndices = indexOps.getAliases();
			oldIndices.keySet().stream()
					.filter(idx -> !idx.equals(newIndexName))
					.forEach(idx -> elasticsearchOperations.indexOps(IndexCoordinates.of(idx)).delete());
		} catch (Exception e) {
			log.warn("Failed to cleanup old indices: {}", e.getMessage());
		}

		log.info("Finished Blue-Green rebuild. Alias '{}' -> '{}'. Total indexed: {}", aliasName, newIndexName, totalIndexed);
	}

	@Override
	public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable)
	{
		if (StrUtil.isBlank(keyword))
		{
			return ApiResponse.success(PageResult.of(postSearchRepository.findAll(pageable)));
		}

		var query = NativeQuery.builder()
				.withQuery(q -> q.multiMatch(m -> m.fields("title^8", "summary^3", "content^1")
						.query(keyword)
						.fuzziness("AUTO")
						.type(TextQueryType.BestFields)))
				.withHighlightQuery(new HighlightQuery(new Highlight(List.of(
						new HighlightField("title"),
						new HighlightField("summary"),
						new HighlightField("content")
				)), PostDocument.class))
				.withPageable(pageable)
				.build();

		SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);
		
		List<PostDocument> documents = searchHits.getSearchHits().stream()
				.map(hit -> {
					PostDocument doc = hit.getContent();
					var highlights = hit.getHighlightFields();
					
					// 1. Highlight Title if present
					if (highlights.containsKey("title")) {
						doc.setTitle(String.join("", highlights.get("title")));
					}
					
					// 2. Highlight Summary if present, otherwise fall back to Content highlight fragment
					if (highlights.containsKey("summary")) {
						doc.setSummary(String.join(" ... ", highlights.get("summary")));
					} else if (highlights.containsKey("content")) {
						doc.setSummary("... " + String.join(" ... ", highlights.get("content")) + " ...");
					}
					
					return doc;
				})
				.collect(Collectors.toList());

		return ApiResponse.success(PageResult.of(new org.springframework.data.domain.PageImpl<>(
				documents, pageable, searchHits.getTotalHits())));
	}

	@Override
	public ApiResponse<List<String>> getSearchSuggestions(String keyword) {
		if (StrUtil.isBlank(keyword) || keyword.length() < 2) {
			return ApiResponse.success(List.of());
		}

		var query = NativeQuery.builder()
				.withQuery(q -> q.matchPhrasePrefix(m -> m.field("title").query(keyword)))
				.withPageable(org.springframework.data.domain.PageRequest.of(0, 10))
				.build();

		SearchHits<PostDocument> hits = elasticsearchOperations.search(query, PostDocument.class);
		List<String> suggestions = hits.getSearchHits().stream()
				.map(hit -> hit.getContent().getTitle())
				.distinct()
				.toList();

		return ApiResponse.success(suggestions);
	}

	@Override
	protected List<QuickSearchResponse.SearchResultItem> searchQuickPosts(String keyword)
	{
		var query = NativeQuery.builder()
				.withQuery(q -> q.multiMatch(m -> m.fields("title^3", "summary").query(keyword)))
				.withPageable(org.springframework.data.domain.PageRequest.of(0, 5))
				.build();

		SearchHits<PostDocument> hits = elasticsearchOperations.search(query, PostDocument.class);
		return hits.getSearchHits().stream()
				.map(hit -> new QuickSearchResponse.SearchResultItem(hit.getContent().getId(), hit.getContent().getTitle(), "/posts/" + hit.getContent().getSlug()))
				.toList();
	}

	@Override
	protected void searchPostsProfessional(String keyword, List<UnifiedSearchResponse.SearchGroup> groups)
	{
		var query = NativeQuery.builder()
				.withQuery(q -> q.multiMatch(m -> m.fields("title^8", "summary^3", "content^1").query(keyword).fuzziness("AUTO")))
				.withHighlightQuery(new HighlightQuery(new Highlight(List.of(
						new HighlightField("title"),
						new HighlightField("summary"),
						new HighlightField("content")
				)), PostDocument.class))
				.withPageable(org.springframework.data.domain.PageRequest.of(0, 5))
				.build();

		SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);

		List<UnifiedSearchResponse.SearchResultItem> items = searchHits.getSearchHits().stream()
				.map(hit -> {
					PostDocument p = hit.getContent();
					String displayTitle = p.getTitle();
					String displayDesc = p.getSummary();
					
					var highlights = hit.getHighlightFields();
					if (highlights.containsKey("title")) displayTitle = String.join("", highlights.get("title"));
					if (highlights.containsKey("summary")) {
						displayDesc = String.join(" ... ", highlights.get("summary"));
					} else if (highlights.containsKey("content")) {
						displayDesc = "... " + String.join(" ... ", highlights.get("content")) + " ...";
					}

					return UnifiedSearchResponse.SearchResultItem.builder()
							.id("post:" + p.getId())
							.title(displayTitle)
							.subtitle(p.getPublishedAt() != null ? p.getPublishedAt().toLocalDate().toString() : "")
							.description(displayDesc)
							.url("/post/" + p.getSlug())
							.icon("book-text")
							.iconColor("#3b82f6")
							.type("POST")
							.score((double) hit.getScore())
							.build();
				})
				.toList();

		if (!items.isEmpty())
		{
			groups.add(UnifiedSearchResponse.SearchGroup.builder().type("POST").label("Articles").priority(10)
					.items(items).build());
		}
	}
}
