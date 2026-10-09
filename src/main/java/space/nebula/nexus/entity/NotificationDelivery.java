package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sys_notification_delivery")
public class NotificationDelivery extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notification_id")
	private Notification notification;

	@Column(name = "deduplication_key", length = 150, unique = true)
	private String deduplicationKey;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(length = 500)
	private String link;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;

	@Column(nullable = false, length = 24)
	private String channel;

	@Column(nullable = false, length = 255)
	private String recipient;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false)
	private Integer attempts = 0;

	@Column(name = "max_attempts", nullable = false)
	private Integer maxAttempts = 5;
	@Column(name = "enqueue_generation", nullable = false)
	private Integer enqueueGeneration = 0;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;
}
