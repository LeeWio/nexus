package space.nebula.nexus.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

@ExtendWith(MockitoExtension.class)
class ElasticsearchPostSearchServiceImplTest {

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

	private ElasticsearchPostSearchServiceImpl postSearchService;

	@BeforeEach
	void setUp() {
		postSearchService = new ElasticsearchPostSearchServiceImpl(postRepository, categoryRepository, tagRepository,
				projectRepository, momentRepository, postSearchRepository, elasticsearchOperations);
	}

	@Test
	void indexesTheCurrentPostStateInsteadOfTheDetachedEventEntity() {
		Post eventPost = post(11L, "Stale title", PostStatus.PUBLISHED);
		Post currentPost = post(11L, "Current title", PostStatus.PUBLISHED);
		currentPost.setSlug("current-title");
		currentPost.setContent("Current content");
		when(postRepository.findPostForSearchIndexing(11L)).thenReturn(Optional.of(currentPost));

		postSearchService.indexPost(eventPost);

		ArgumentCaptor<PostDocument> documentCaptor = ArgumentCaptor.forClass(PostDocument.class);
		verify(postSearchRepository).save(documentCaptor.capture());
		assertEquals("Current title", documentCaptor.getValue().getTitle());
		assertEquals("current-title", documentCaptor.getValue().getSlug());
	}

	@Test
	void removesTheSearchDocumentWhenTheCurrentPostIsNoLongerPublished() {
		Post eventPost = post(12L, "Previously published", PostStatus.PUBLISHED);
		Post archivedPost = post(12L, "Archived", PostStatus.ARCHIVED);
		when(postRepository.findPostForSearchIndexing(12L)).thenReturn(Optional.of(archivedPost));

		postSearchService.indexPost(eventPost);

		verify(postSearchRepository).deleteById("12");
		verify(postSearchRepository, never()).save(any(PostDocument.class));
	}

	private Post post(Long id, String title, PostStatus status) {
		Post post = new Post();
		post.setId(id);
		post.setTitle(title);
		post.setSlug(title.toLowerCase().replace(' ', '-'));
		post.setContent("content");
		post.setStatus(status);
		return post;
	}
}
