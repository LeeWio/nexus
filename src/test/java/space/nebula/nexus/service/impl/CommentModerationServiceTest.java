package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.repository.CommentRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentModerationServiceTest {
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private CommentGovernanceService governanceService;
	@Mock
	private CommentMetricsService metricsService;

	@Test
	void deletedPlaceholderCannotBePinnedAgain() {
		Comment comment = new Comment();
		comment.setId(15L);
		comment.setDeletedPlaceholder(true);
		when(commentRepository.findById(15L)).thenReturn(Optional.of(comment));
		CommentModerationService service = new CommentModerationService(commentRepository, eventPublisher,
				governanceService, metricsService);

		BusinessException exception = assertThrows(BusinessException.class, () -> service.pinComment(15L, true));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).save(comment);
	}
}
