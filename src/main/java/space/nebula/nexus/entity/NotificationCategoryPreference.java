package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import space.nebula.nexus.enums.NotificationCategory;

@Getter
@Setter
@Entity
@Table(name = "sys_notification_category_preference", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id",
		"category"}))
public class NotificationCategoryPreference extends BaseEntity {
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private NotificationCategory category;
	@Column(name = "in_app_enabled", nullable = false)
	private Boolean inAppEnabled;
	@Column(name = "email_enabled", nullable = false)
	private Boolean emailEnabled;
}
