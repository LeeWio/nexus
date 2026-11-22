package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_moment_email_delivery")
public class MomentEmailDelivery extends BaseEntity {
	public enum Status {
		QUEUED, SENDING, FAILED, DELIVERED, CANCELLED, ABANDONED
	}

	@Column(name = "subscriber_id", nullable = false)
	private Long subscriberId;

	// A confirmation email has no Moment.
	@Column(name = "moment_id")
	private Long momentId;

	@Column(nullable = false)
	private long generation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.QUEUED;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;

	@Column(name = "last_error")
	private String lastError;
}
