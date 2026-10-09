package space.nebula.nexus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.NotificationEmailRequestedEvent;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.util.concurrent.ConcurrentLinkedQueue;
import space.nebula.nexus.common.listener.PostWorkflowNotificationListener;
import space.nebula.nexus.entity.NotificationPreference;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.NotificationDeliveryRepository;
import space.nebula.nexus.repository.NotificationPreferenceRepository;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.impl.NotificationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = "spring.test.database.replace=NONE")
@ActiveProfiles("test")
@Import({NotificationServiceImpl.class, NotificationDeliveryService.class, PostWorkflowNotificationListener.class,
		NotificationTransactionIntegrationTest.CommittedEmails.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationTransactionIntegrationTest {
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private ApplicationEventPublisher events;
	@Autowired
	private UserRepository users;
	@Autowired
	private PostRepository posts;
	@Autowired
	private NotificationRepository notifications;
	@Autowired
	private NotificationPreferenceRepository preferences;
	@Autowired
	private NotificationDeliveryRepository deliveries;
	@Autowired
	private INotificationService notificationService;
	@Autowired
	private NotificationDeliveryService deliveryService;
	@Autowired
	private CommittedEmails committedEmails;
	private TransactionTemplate transaction;
	private User reader;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		reader = transaction.execute(status -> {
			User user = new User();
			user.setUsername("notification-test-" + System.nanoTime());
			user.setPassword("password");
			user.setEmail(user.getUsername() + "@example.com");
			user.setCreatedAt(LocalDateTime.now());
			users.save(user);
			NotificationPreference preference = new NotificationPreference();
			preference.setUser(user);
			preference.setSystemEmailEnabled(true);
			preference.setCreatedAt(LocalDateTime.now());
			preferences.save(preference);
			return user;
		});
	}

	@Test
	void businessCommitAlsoCommitsNotificationAndMailSnapshot() {
		transaction.executeWithoutResult(
				status -> events.publishEvent(new PostChangedEvent(this, post(), PostChangeType.PUBLISHED)));
		var inbox = notifications.findInboxByRecipientId(reader.getId(), false, null, PageRequest.of(0, 10));
		assertEquals(1, inbox.getTotalElements());
		var delivery = deliveries.findByNotificationIdAndChannel(inbox.getContent().getFirst().getId(), "EMAIL")
				.orElseThrow();
		assertEquals("QUEUED", delivery.getStatus());
		assertEquals("Post published", delivery.getTitle());
		assertEquals("POST", inbox.getContent().getFirst().getObjectType());
		assertEquals("VIEW", inbox.getContent().getFirst().getAction());
		assertNotNull(delivery.getNextAttemptAt());
		assertTrue(committedEmails.ids.contains(delivery.getId()));
	}

	@Test
	void businessRollbackLeavesNeitherNotificationNorQueuedEmail() {
		int committedBefore = committedEmails.ids.size();
		transaction.executeWithoutResult(status -> {
			events.publishEvent(new PostChangedEvent(this, post(), PostChangeType.PUBLISHED));
			status.setRollbackOnly();
		});
		assertEquals(0, notifications.findInboxByRecipientId(reader.getId(), false, null, PageRequest.of(0, 10))
				.getTotalElements());
		assertEquals(committedBefore, committedEmails.ids.size());
	}

	@Test
	void dismissalPreservesDeduplicationAndPendingDelivery() {
		notificationService.sendOnce(reader, "Result", "Reviewed", NotificationType.POST_REJECTED, null, "event-1");
		var notification = notifications.findByDeduplicationKey("POST_REJECTED:event-1:" + reader.getId())
				.orElseThrow();
		transaction.executeWithoutResult(status -> notifications.deleteOwnedById(notification.getId(), reader.getId()));
		notificationService.sendOnce(reader, "Result", "Reviewed", NotificationType.POST_REJECTED, null, "event-1");
		assertTrue(notifications.findByIdAndRecipientId(notification.getId(), reader.getId()).isEmpty());
		assertEquals(0, notifications.countByRecipientIdAndIsVisibleTrueAndIsReadFalse(reader.getId()));
		assertTrue(deliveries.findByNotificationIdAndChannel(notification.getId(), "EMAIL").isPresent());
	}

	@Test
	void concurrentEventsProduceOneNotificationAndOneDelivery() throws Exception {
		try (var executor = Executors.newFixedThreadPool(4)) {
			Callable<Void> generate = () -> {
				notificationService.sendOnce(reader, "Result", "Reviewed", NotificationType.POST_REJECTED, null,
						"concurrent");
				return null;
			};
			for (var result : executor.invokeAll(List.of(generate, generate, generate, generate)))
				result.get();
		}
		assertEquals(1, notifications.findInboxByRecipientId(reader.getId(), false, null, PageRequest.of(0, 10))
				.getTotalElements());
		var notification = notifications.findByDeduplicationKey("POST_REJECTED:concurrent:" + reader.getId())
				.orElseThrow();
		assertTrue(deliveries.findByNotificationIdAndChannel(notification.getId(), "EMAIL").isPresent());
	}

	@Test
	void concurrentBrokerConsumersCanOnlyClaimOneAttempt() throws Exception {
		notificationService.sendOnce(reader, "Result", "Reviewed", NotificationType.POST_REJECTED, null, "claim");
		var notification = notifications.findByDeduplicationKey("POST_REJECTED:claim:" + reader.getId()).orElseThrow();
		Long id = deliveries.findByNotificationIdAndChannel(notification.getId(), "EMAIL").orElseThrow().getId();
		try (var executor = Executors.newFixedThreadPool(4)) {
			Callable<Boolean> claim = () -> deliveryService.beginDelivery(id).isPresent();
			int claimed = 0;
			for (var result : executor.invokeAll(List.of(claim, claim, claim, claim)))
				if (result.get())
					claimed++;
			assertEquals(1, claimed);
		}
		assertEquals(1, deliveries.findById(id).orElseThrow().getAttempts());
	}

	private Post post() {
		Post post = new Post();
		post.setTitle("Example");
		post.setSlug("notification-post-" + System.nanoTime());
		post.setContent("{}");
		post.setAuthor(users.getReferenceById(reader.getId()));
		post.setStatus(PostStatus.PUBLISHED);
		post.setCreatedAt(LocalDateTime.now());
		return posts.save(post);
	}

	static class CommittedEmails {
		final ConcurrentLinkedQueue<Long> ids = new ConcurrentLinkedQueue<>();
		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onEmailRequested(NotificationEmailRequestedEvent event) {
			ids.add(event.deliveryId());
		}
	}
}
