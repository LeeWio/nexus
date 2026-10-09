package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.FriendLinkProperties;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.mapper.FriendLinkMapper;
import space.nebula.nexus.payload.request.FriendLinkApplicationRequest;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.service.NotificationDeliveryService;
import space.nebula.nexus.enums.NotificationType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkServiceImplTest {

	@Mock
	private FriendLinkRepository friendLinkRepository;
	@Mock
	private FriendLinkMapper friendLinkMapper;
	@Mock
	private INotificationService notificationService;
	@Mock
	private NotificationDeliveryService notificationDeliveryService;
	@Mock
	private FriendLinkProperties friendLinkProperties;
	@InjectMocks
	private FriendLinkServiceImpl service;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient().when(friendLinkProperties.getModerationEmail())
				.thenReturn("moderator@example.com");
	}

	@Test
	void applicationNormalizesUrlAndPersistsNotificationRequests() {
		FriendLinkApplicationRequest request = new FriendLinkApplicationRequest(" Example ", "HTTPS://Example.COM/",
				null, " Site ", "Owner@Example.COM");
		when(friendLinkRepository.findByUrl("https://example.com")).thenReturn(Optional.empty());
		when(friendLinkRepository.save(any(FriendLink.class))).thenAnswer(invocation -> {
			FriendLink link = invocation.getArgument(0);
			link.setId(1L);
			return link;
		});

		var response = service.applyForFriendLink(request);

		assertEquals(200, response.code());
		verify(friendLinkRepository).save(org.mockito.ArgumentMatchers.argThat(
				link -> "https://example.com".equals(link.getUrl()) && link.getStatus() == FriendLinkStatus.APPLYING
						&& !link.getIsPublished() && "owner@example.com".equals(link.getEmail())));
		verify(notificationService).sendToAdministrators("Friend link application received",
				"\"Example\" applied for a friend link and needs review.", NotificationType.FRIEND_LINK_APPLICATION,
				"/links?id=1", "FRIEND_LINK:1", null,
				new space.nebula.nexus.payload.response.NotificationContext(
						space.nebula.nexus.payload.response.NotificationContext.ObjectType.FRIEND_LINK, 1L, null,
						space.nebula.nexus.payload.response.NotificationContext.Action.REVIEW));
		verify(notificationDeliveryService).queueExternalEmail(org.mockito.ArgumentMatchers.eq("moderator@example.com"),
				any(String.class), any(String.class), org.mockito.ArgumentMatchers.eq("/links?id=1"),
				org.mockito.ArgumentMatchers.eq("FRIEND_LINK_MODERATION:1"));
	}

	@Test
	void applicationRejectsDangerousUrlScheme() {
		FriendLinkApplicationRequest request = new FriendLinkApplicationRequest("Unsafe", "javascript:alert(1)", null,
				null, "owner@example.com");

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

		assertThrows(BusinessException.class, () -> service.moderateFriendLink(1L, FriendLinkStatus.APPLYING));

		assertFalse(link.getIsPublished());
		verify(friendLinkRepository, never()).save(link);
	}

	@Test
	void moderationNotifiesApplicantOfApproval() {
		FriendLink link = new FriendLink();
		link.setId(1L);
		link.setName("Example");
		link.setEmail("owner@example.com");
		link.setStatus(FriendLinkStatus.APPLYING);
		when(friendLinkRepository.findById(1L)).thenReturn(Optional.of(link));

		service.moderateFriendLink(1L, FriendLinkStatus.APPROVED);

		verify(notificationDeliveryService).queueExternalEmail("owner@example.com", "Friend link application approved",
				"Your application for \"Example\" has been reviewed. Result: APPROVED.", null,
				"FRIEND_LINK_APPROVED:1");
	}
}
