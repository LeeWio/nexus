package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostSearchServiceTest {

	@Mock
	private PostRepository postRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TagRepository tagRepository;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private MomentRepository momentRepository;
	@Mock
	private PostSearchRepository postSearchRepository;
	@Mock
	private ElasticsearchOperations elasticsearchOperations;

	@Mock
	private SearchHits<PostDocument> searchHits;
	@Mock
	private SearchHit<PostDocument> searchHit;

	@InjectMocks
	private ElasticsearchPostSearchServiceImpl searchService;

	@Test
	void getSearchSuggestions_EmptyKeyword() {
		var response = searchService.getSearchSuggestions("");
		assertTrue(response.data().isEmpty());
	}

	@Test
	void getSearchSuggestions_ShortKeyword() {
		var response = searchService.getSearchSuggestions("a");
		assertTrue(response.data().isEmpty());
	}

	@Test
	void unifiedSearchReturnsPlainTextAndCanonicalArticlePath() {
		var post = PostDocument.builder().id("12").slug("一 篇").title("人是无法享受孤独的").build();
		when(elasticsearchOperations.search(any(NativeQuery.class), eq(PostDocument.class))).thenReturn(searchHits);
		when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
		when(searchHit.getContent()).thenReturn(post);
		when(searchHit.getHighlightFields()).thenReturn(Map.of("summary", List.of("关于<em>孤独</em>的文章")));

		var result = searchService.unifiedSearch("孤独").data().getGroups().getFirst().getItems().getFirst();

		assertEquals("人是无法享受孤独的", result.getTitle());
		assertEquals("关于孤独的文章", result.getDescription());
		assertEquals("/single/%E4%B8%80%20%E7%AF%87", result.getUrl());
	}
}
