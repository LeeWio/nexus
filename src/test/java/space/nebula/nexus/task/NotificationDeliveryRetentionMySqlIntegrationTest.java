package space.nebula.nexus.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.repository.search.PostSearchRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.notification-delivery.retention.delivery-days=90",
		"app.notification-delivery.retention.retry-audit-days=180", "app.notification-delivery.retention.batch-size=1"})
@EnabledIfSystemProperty(named = "spring.datasource.driver-class-name", matches = "com.mysql.cj.jdbc.Driver")
class NotificationDeliveryRetentionMySqlIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private NotificationDeliveryRetentionTask retentionTask;

	@BeforeEach
	void cleanFixture() {
		jdbc.update("DELETE FROM sys_notification_delivery_retry");
		jdbc.update("DELETE FROM sys_notification_delivery");
	}

	@Test
	void deletesOnlyExpiredTerminalDeliveriesAndRetryAuditsInMySql() {
		LocalDateTime now = LocalDateTime.now();
		long expiredDelivered = insertDelivery("expired-delivered", "DELIVERED", now.minusDays(120));
		insertDelivery("expired-failed", "FAILED", now.minusDays(120));
		insertDelivery("expired-queued", "QUEUED", now.minusDays(120));
		long recentDelivered = insertDelivery("recent-delivered", "DELIVERED", now.minusDays(1));
		insertRetry(expiredDelivered, "expired-request", now.minusDays(200));
		insertRetry(recentDelivered, "recent-request", now.minusDays(1));

		retentionTask.purgeExpiredRecords();

		assertEquals(0, countDelivery("expired-delivered"));
		assertEquals(0, countDelivery("expired-failed"));
		assertEquals(1, countDelivery("expired-queued"));
		assertEquals(1, countDelivery("recent-delivered"));
		assertEquals(0, countRetry("expired-request"));
		assertEquals(1, countRetry("recent-request"));
	}

	private long insertDelivery(String key, String status, LocalDateTime createdAt) {
		jdbc.update("""
				INSERT INTO sys_notification_delivery
				(notification_id, channel, recipient, title, content, link, deduplication_key,
				 status, attempts, max_attempts, enqueue_generation, next_attempt_at,
				 created_at, updated_at, is_deleted)
				VALUES (NULL, 'EMAIL', 'reader@example.test', 'Fixture', 'Fixture body',
				 '/single/fixture', ?, ?, 0, 5, 0, ?, ?, ?, false)
				""", key, status, createdAt, createdAt, createdAt);
		return jdbc.queryForObject("SELECT id FROM sys_notification_delivery WHERE deduplication_key = ?", Long.class,
				key);
	}

	private void insertRetry(long deliveryId, String requestId, LocalDateTime createdAt) {
		jdbc.update("""
				INSERT INTO sys_notification_delivery_retry
				(delivery_id, request_id, requested_by, reason, previous_status,
				 previous_max_attempts, new_max_attempts, attempts_at_retry,
				 created_at, updated_at, is_deleted)
				VALUES (?, ?, 'e2e-admin', 'Fixture retry', 'FAILED', 5, 5, 1, ?, ?, false)
				""", deliveryId, requestId, createdAt, createdAt);
	}

	private int countDelivery(String key) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM sys_notification_delivery WHERE deduplication_key = ?",
				Integer.class, key);
	}

	private int countRetry(String requestId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM sys_notification_delivery_retry WHERE request_id = ?",
				Integer.class, requestId);
	}
}
