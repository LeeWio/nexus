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
 * A post that the user wants to revisit later.
 */
@Getter
@Setter
@Entity
@Table(name = "blog_reading_list_item", uniqueConstraints = @UniqueConstraint(name = "uk_reading_list_user_post", columnNames = {
		"user_id", "post_id"}))
public class ReadingListItem extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;
}
