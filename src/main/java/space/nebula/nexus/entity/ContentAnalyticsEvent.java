package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.ContentAnalyticsEventType;

/** A de-duplicated anonymous engagement milestone for a published post. */
@Getter
@Setter
@Entity
@Table(name = "blog_content_analytics_event", uniqueConstraints = @UniqueConstraint(name = "uk_content_analytics_session_post_event", columnNames = {
		"session_id", "post_id", "event_type"}))
@SQLRestriction("is_deleted = false")
public class ContentAnalyticsEvent extends BaseEntity {

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "visitor_hash", nullable = false, length = 64)
	private String visitorHash;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private ContentAnalyticsEventType eventType;

	@Column(name = "progress_percent")
	private Integer progressPercent;

	@Column(name = "active_seconds")
	private Integer activeSeconds;
}
