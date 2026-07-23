package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

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
}
