package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One scheduled newsletter run and its aggregate delivery lifecycle. */
@Getter
@Setter
@Entity
@Table(name = "blog_newsletter_delivery_batch")
public class NewsletterDeliveryBatch extends BaseEntity {
	@Column(nullable = false, length = 24)
	private String status;

	@Column(name = "recipient_count", nullable = false)
	private Integer recipientCount = 0;

	@Column(name = "queued_count", nullable = false)
	private Integer queuedCount = 0;

	@Column(name = "delivered_count", nullable = false)
	private Integer deliveredCount = 0;

	@Column(name = "failed_count", nullable = false)
	private Integer failedCount = 0;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;
}
