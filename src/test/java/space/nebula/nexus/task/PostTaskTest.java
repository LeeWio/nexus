package space.nebula.nexus.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.service.IPostPublicationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for scheduled post publication orchestration.
 */
@ExtendWith(MockitoExtension.class)
class PostTaskTest {
	@Mock
	private IPostPublicationService publicationService;

	@InjectMocks
	private PostTask postTask;

	@Test
	void delegatesDuePublicationToBusinessService() {
		postTask.publishScheduledPosts();

		verify(publicationService).publishDueScheduledPosts(any(LocalDateTime.class), eq(100));
	}
}
