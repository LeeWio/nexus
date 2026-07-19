package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.FriendLinkProperties;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkApplicationRequest;
import space.nebula.nexus.repository.FriendLinkRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkServiceImplTest {

	@Mock private FriendLinkRepository friendLinkRepository;
	@Mock private FriendLinkMapper friendLinkMapper;
	@Mock private RabbitTemplate rabbitTemplate;
	@Mock private FriendLinkProperties friendLinkProperties;
	@InjectMocks private FriendLinkServiceImpl service;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient().when(friendLinkProperties.getModerationEmail())
				.thenReturn("moderator@example.com");
	}

	@Test
	void applicationNormalizesUrlAndSurvivesNotificationFailure() {
		FriendLinkApplicationRequest request = new FriendLinkApplicationRequest(
				" Example ", "HTTPS://Example.COM/", null, " Site ", "Owner@Example.COM");
		when(friendLinkRepository.findByUrl("https://example.com")).thenReturn(Optional.empty());
		when(friendLinkRepository.save(any(FriendLink.class))).thenAnswer(invocation -> {
			FriendLink link = invocation.getArgument(0);
			link.setId(1L);
			return link;
		});
		doThrow(new RuntimeException("broker unavailable")).when(rabbitTemplate)
				.convertAndSend(any(String.class), any(String.class), any(Object.class));

		var response = service.applyForFriendLink(request);

		assertEquals(200, response.code());
		verify(friendLinkRepository).save(org.mockito.ArgumentMatchers.argThat(link ->
				"https://example.com".equals(link.getUrl())
						&& link.getStatus() == FriendLinkStatus.APPLYING
						&& !link.getIsPublished()
						&& "owner@example.com".equals(link.getEmail())));
	}

	@Test
	void applicationRejectsDangerousUrlScheme() {
		FriendLinkApplicationRequest request = new FriendLinkApplicationRequest(
				"Unsafe", "javascript:alert(1)", null, null, "owner@example.com");

		assertThrows(BusinessException.class, () -> service.applyForFriendLink(request));

		verify(friendLinkRepository, never()).save(any());
	}

	@Test
	void moderationRejectsReturningApplicationToPending() {
		FriendLink link = new FriendLink();
		link.setId(1L);
		link.setStatus(FriendLinkStatus.APPLYING);
		link.setIsPublished(false);
		when(friendLinkRepository.findById(1L)).thenReturn(Optional.of(link));

		assertThrows(BusinessException.class,
				() -> service.moderateFriendLink(1L, FriendLinkStatus.APPLYING));

		assertFalse(link.getIsPublished());
		verify(friendLinkRepository, never()).save(link);
	}
}
