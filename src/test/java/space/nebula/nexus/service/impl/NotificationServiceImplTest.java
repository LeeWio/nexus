package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.NotificationPreference;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.NotificationPreferenceRequest;
import space.nebula.nexus.repository.NotificationPreferenceRepository;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.NotificationDeliveryService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PostRepository postRepository;

	@Mock
	private NotificationDeliveryService notificationDeliveryService;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private User currentUser;

	@BeforeEach
	void setUp() {
		currentUser = new User();
		currentUser.setId(42L);
		currentUser.setUsername("reader");
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("reader", "password", List.of()));
		lenient().when(userRepository.findByUsername("reader")).thenReturn(Optional.of(currentUser));
		lenient().when(notificationRepository.findCategoryPublicationEmailNotifications(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(List.of());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void categoryPublicationUsesOneIdempotentBulkInsert() {
		Category category = new Category();
		category.setId(3L);
		category.setName("Architecture");
		User author = new User();
		author.setId(9L);
		Post post = new Post();
		post.setId(7L);
		post.setTitle("Designing Reliable Services");
		post.setSlug("designing-reliable-services");
		post.setStatus(PostStatus.PUBLISHED);
		post.setCategory(category);
		post.setAuthor(author);
		when(postRepository.findPublicationNotificationPost(7L)).thenReturn(Optional.of(post));
		when(notificationRepository.insertCategoryPublicationNotifications(3L, 9L, 7L, "New post in Architecture",
				"\"Designing Reliable Services\" is now available in a category you follow.",
				"/post/designing-reliable-services")).thenReturn(12);

		int recipients = notificationService.sendCategoryPublication(7L);

		assertEquals(12, recipients);
		verify(notificationRepository).insertCategoryPublicationNotifications(3L, 9L, 7L, "New post in Architecture",
				"\"Designing Reliable Services\" is now available in a category you follow.",
				"/post/designing-reliable-services");
	}

	@Test
	void getMyNotificationsReadsTheInboxByDefault() {
		var pageable = PageRequest.of(0, 10);
		when(notificationRepository.findInboxByRecipientId(42L, false, pageable))
				.thenReturn(new PageImpl<>(List.of()));

		notificationService.getMyNotifications(false, "inbox", pageable);

		verify(notificationRepository).findInboxByRecipientId(42L, false, pageable);
		verify(notificationRepository, never()).findByRecipientId(42L, pageable);
	}

	@Test
	void markAsReadRecordsReadTimestamp() {
		Notification notification = new Notification();
		notification.setIsRead(false);
		when(notificationRepository.findByIdAndRecipientId(7L, 42L)).thenReturn(Optional.of(notification));

		notificationService.markAsRead(7L);

		assertTrue(notification.getIsRead());
		assertNotNull(notification.getReadAt());
		verify(notificationRepository).save(notification);
	}

	@Test
	void deleteNotificationRejectsMissingOrForeignNotification() {
		when(notificationRepository.deleteOwnedById(7L, 42L)).thenReturn(0);

		assertThrows(ResourceNotFoundException.class, () -> notificationService.deleteNotification(7L));
	}

	@Test
	void clearReadNotificationsDeletesOnlyOwnedReadItems() {
		notificationService.clearReadNotifications();

		verify(notificationRepository).deleteReadByRecipientId(42L);
	}

	@Test
	void getMyPreferencesDefaultsToEveryCategoryEnabled() {
		when(notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(42L)).thenReturn(Optional.empty());

		var response = notificationService.getMyPreferences();

		assertTrue(response.data().commentNotificationsEnabled());
		assertTrue(response.data().categoryPostNotificationsEnabled());
		assertTrue(response.data().systemNotificationsEnabled());
	}

	@Test
	void updateMyPreferencesCreatesAndReturnsCompleteConfiguration() {
		when(notificationPreferenceRepository.findByUserId(42L)).thenReturn(Optional.empty());
		when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = notificationService.updateMyPreferences(
				new NotificationPreferenceRequest(false, true, false, true, false, true));

		assertTrue(!response.data().commentNotificationsEnabled());
		assertTrue(response.data().categoryPostNotificationsEnabled());
		assertTrue(!response.data().systemNotificationsEnabled());
		var savedPreference = org.mockito.ArgumentCaptor.forClass(NotificationPreference.class);
		verify(notificationPreferenceRepository).save(savedPreference.capture());
		assertEquals(currentUser, savedPreference.getValue().getUser());
		assertTrue(!savedPreference.getValue().getCommentEnabled());
		assertTrue(savedPreference.getValue().getCategoryPostEnabled());
		assertTrue(!savedPreference.getValue().getSystemEnabled());
		assertTrue(savedPreference.getValue().getCommentEmailEnabled());
		assertTrue(!savedPreference.getValue().getCategoryPostEmailEnabled());
		assertTrue(savedPreference.getValue().getSystemEmailEnabled());
	}

	@Test
	void sendSkipsDisabledCommentNotifications() {
		NotificationPreference preference = new NotificationPreference();
		preference.setCommentEnabled(false);
		when(notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(42L)).thenReturn(Optional.of(preference));

		notificationService.send(currentUser, "New reply", "A reader replied to your comment.", "COMMENT_REPLY",
				"/posts/example#comment-1");

		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void sendDeliversSystemNotificationsWhenNoPreferenceExists() {
		when(notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(42L)).thenReturn(Optional.empty());

		notificationService.send(currentUser, "Broken links", "One link needs review.", "HEALTH_CHECK",
				"/admin/content/links");

		verify(notificationRepository).save(any(Notification.class));
	}

	@Test
	void sendKeepsEmailOnlyNotificationsOutOfTheInAppInbox() {
		NotificationPreference preference = new NotificationPreference();
		preference.setCommentEnabled(false);
		preference.setCommentEmailEnabled(true);
		when(notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(42L)).thenReturn(Optional.of(preference));

		notificationService.send(currentUser, "New reply", "A reader replied to your comment.", "COMMENT_REPLY",
				"/posts/example#comment-1");

		var savedNotification = org.mockito.ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(savedNotification.capture());
		assertTrue(!savedNotification.getValue().getIsVisible());
		verify(notificationDeliveryService).queueEmail(savedNotification.getValue(), currentUser.getEmail(), "New reply",
				"A reader replied to your comment.", "/posts/example#comment-1");
	}

	@Test
	void markAsDoneKeepsNotificationInHistory() {
		Notification notification = new Notification();
		notification.setIsRead(false);
		when(notificationRepository.findByIdAndRecipientId(7L, 42L)).thenReturn(Optional.of(notification));

		notificationService.markAsDone(7L);

		assertNotNull(notification.getCompletedAt());
		assertTrue(notification.getIsRead());
		assertNotNull(notification.getReadAt());
		verify(notificationRepository).save(notification);
	}

	@Test
	void setSavedPersistsTheTriagedState() {
		Notification notification = new Notification();
		when(notificationRepository.findByIdAndRecipientId(7L, 42L)).thenReturn(Optional.of(notification));

		notificationService.setSaved(7L, true);

		assertTrue(notification.getIsSaved());
		verify(notificationRepository).save(notification);
	}

	@Test
	void reopenReturnsCompletedNotificationToTheInbox() {
		Notification notification = new Notification();
		notification.setCompletedAt(java.time.LocalDateTime.now());
		when(notificationRepository.findByIdAndRecipientId(7L, 42L)).thenReturn(Optional.of(notification));

		notificationService.reopen(7L);

		assertTrue(notification.getCompletedAt() == null);
		verify(notificationRepository).save(notification);
	}
}
