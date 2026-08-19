package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import space.nebula.nexus.enums.SubscriberStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_subscriber")
public class Subscriber extends BaseEntity {

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriberStatus status = SubscriberStatus.PENDING;

	@Column(name = "verification_token", length = 100)
	private String verificationToken;

	@Column(name = "verification_expires_at")
	private LocalDateTime verificationExpiresAt;

	@Column(name = "unsubscribe_token", length = 100)
	private String unsubscribeToken;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;
}
