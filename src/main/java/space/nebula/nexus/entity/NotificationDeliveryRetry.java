package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_notification_delivery_retry", uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id",
		"request_id"}))
public class NotificationDeliveryRetry extends BaseEntity {
	@Column(name = "delivery_id", nullable = false)
	private Long deliveryId;
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;
	@Column(name = "requested_by", nullable = false)
	private String requestedBy;
	@Column(nullable = false, length = 500)
	private String reason;
	@Column(name = "previous_status", nullable = false, length = 24)
	private String previousStatus;
	@Column(name = "previous_max_attempts", nullable = false)
	private Integer previousMaxAttempts;
	@Column(name = "new_max_attempts", nullable = false)
	private Integer newMaxAttempts;
	@Column(name = "attempts_at_retry", nullable = false)
	private Integer attemptsAtRetry;
}
