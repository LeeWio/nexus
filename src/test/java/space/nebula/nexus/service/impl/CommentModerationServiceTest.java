package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.CommentRepository;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@ParameterizedTest
	@EnumSource(value = CommentStatus.class, names = {"APPROVED", "REJECTED", "SPAM"})
	void guestCommentCanBeModerated(CommentStatus status) {
		Comment comment = guestComment(48L);
		when(commentRepository.findById(48L)).thenReturn(Optional.of(comment));
		CommentModerationService service = new CommentModerationService(commentRepository, eventPublisher,
				governanceService, metricsService);

		service.moderateComment(48L, status);

		ArgumentCaptor<CommentModeratedEvent> event = ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		verify(commentRepository).save(comment);
		assertEquals(status, comment.getStatus());
		assertNull(event.getValue().getAuthorId());
		assertNull(event.getValue().getReplyRecipientId());
		assertEquals("Guest reader", event.getValue().getAuthorUsername());
		assertEquals(status == CommentStatus.APPROVED ? "/guestbook#comment-48" : null, event.getValue().getLink());
	}

	@Test
	void registeredReplyToGuestParentCanBeApproved() {
		Comment parent = guestComment(47L);
		parent.setStatus(CommentStatus.APPROVED);
		Comment comment = new Comment();
		comment.setId(48L);
		comment.setStatus(CommentStatus.PENDING);
		comment.setParent(parent);
		User author = new User();
		author.setId(5L);
		author.setUsername("reader");
		comment.setUser(author);
		when(commentRepository.findById(48L)).thenReturn(Optional.of(comment));
		CommentModerationService service = new CommentModerationService(commentRepository, eventPublisher,
				governanceService, metricsService);

		service.moderateComment(48L, CommentStatus.APPROVED);

		ArgumentCaptor<CommentModeratedEvent> event = ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		assertEquals(CommentStatus.APPROVED, comment.getStatus());
		assertEquals(5L, event.getValue().getAuthorId());
		assertEquals("reader", event.getValue().getAuthorUsername());
		assertNull(event.getValue().getReplyRecipientId());
	}

	@Test
	void guestReplyToRegisteredParentCanBeBatchApproved() {
		User recipient = new User();
		recipient.setId(5L);
		Comment parent = new Comment();
		parent.setUser(recipient);
		parent.setStatus(CommentStatus.APPROVED);
		Comment comment = guestComment(48L);
		comment.setParent(parent);
		when(commentRepository.findById(48L)).thenReturn(Optional.of(comment));
		CommentModerationService service = new CommentModerationService(commentRepository, eventPublisher,
				governanceService, metricsService);

		service.batchModerateComments(List.of(48L), CommentStatus.APPROVED);

		ArgumentCaptor<CommentModeratedEvent> event = ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		assertEquals(CommentStatus.APPROVED, comment.getStatus());
		assertNull(event.getValue().getAuthorId());
		assertEquals(5L, event.getValue().getReplyRecipientId());
	}

	private Comment guestComment(Long id) {
		Comment comment = new Comment();
		comment.setId(id);
		comment.setStatus(CommentStatus.PENDING);
		comment.setGuestName("Guest reader");
		return comment;
	}

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
