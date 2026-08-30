package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Durable delivery outcome for one recipient in a newsletter batch. */
@Getter
@Setter
@Entity
@Table(name = "blog_newsletter_delivery")
public class NewsletterDelivery extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private NewsletterDeliveryBatch batch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false)
	private Subscriber subscriber;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false)
	private Integer attempts = 0;

	@Column(nullable = false)
	private String recipient;

	@Column(nullable = false)
	private String subject;

	@Column(name = "template_name", nullable = false)
	private String templateName;

	@Lob
	@Column(name = "template_variables", nullable = false, columnDefinition = "TEXT")
	private String templateVariables;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;
}
