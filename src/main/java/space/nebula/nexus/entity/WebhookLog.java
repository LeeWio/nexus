package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_webhook_log")
public class WebhookLog extends BaseEntity {

	@Column(name = "delivery_id", nullable = false, unique = true, length = 64)
	private String deliveryId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "webhook_id", nullable = false)
	private Webhook webhook;

	@Column(nullable = false)
	private String event;

	@Column(columnDefinition = "TEXT")
	private String url;

	@Column(columnDefinition = "TEXT")
	private String requestPayload;

	@Column(columnDefinition = "TEXT")
	private String responsePayload;

	private Integer responseCode;

	private Boolean isSuccess;

	private String errorMessage;

	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount = 1;

	// An unfinished attempt can be recovered after a worker or API process exits.
	public boolean isInProgress(LocalDateTime now) {
		return isSuccess == null && getUpdatedAt() != null && getUpdatedAt().plusMinutes(5).isAfter(now);
	}
}
