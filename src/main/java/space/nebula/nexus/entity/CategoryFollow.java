package space.nebula.nexus.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Explicit category preference owned by a user.
 */
@Getter
@Setter
@Entity
@Table(name = "blog_category_follow", uniqueConstraints = @UniqueConstraint(name = "uk_category_follow_user_category", columnNames = {
		"user_id", "category_id"}))
public class CategoryFollow extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;
}
