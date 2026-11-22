package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import space.nebula.nexus.enums.SubscriberStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_moment_subscriber")
public class MomentSubscriber extends BaseEntity {
	@Column(nullable = false, length = 254, unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriberStatus status;

	@Column(nullable = false, length = 2)
	private String language;

	@Column(nullable = false)
	private long generation;

	@Column(name = "verification_token", length = 36, unique = true)
	private String verificationToken;

	@Column(name = "verification_requested_at")
	private LocalDateTime verificationRequestedAt;

	@Column(name = "verification_expires_at")
	private LocalDateTime verificationExpiresAt;

	@Column(name = "unsubscribe_token", length = 36, unique = true)
	private String unsubscribeToken;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;
}
