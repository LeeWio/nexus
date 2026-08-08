package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-user controls for in-app notification categories.
 */
@Getter
@Setter
@Entity
@Table(name = "sys_notification_preference", uniqueConstraints = @UniqueConstraint(name = "uk_notification_preference_user", columnNames = "user_id"))
public class NotificationPreference extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "comment_enabled", nullable = false)
	private Boolean commentEnabled = true;

	@Column(name = "category_post_enabled", nullable = false)
	private Boolean categoryPostEnabled = true;

	@Column(name = "system_enabled", nullable = false)
	private Boolean systemEnabled = true;
}
