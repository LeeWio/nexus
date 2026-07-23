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
 * User-owned named collection of posts.
 */
@Getter
@Setter
@Entity
@Table(name = "blog_post_collection", uniqueConstraints = @UniqueConstraint(name = "uk_post_collection_user_name", columnNames = {
		"user_id", "name"}))
public class PostCollection extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(length = 300)
	private String description;
}
