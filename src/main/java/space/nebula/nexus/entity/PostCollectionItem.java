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
 * Membership of a post in a user-owned collection.
 */
@Getter
@Setter
@Entity
@Table(name = "blog_post_collection_item", uniqueConstraints = @UniqueConstraint(name = "uk_collection_item_collection_post", columnNames = {
		"collection_id", "post_id"}))
public class PostCollectionItem extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "collection_id", nullable = false)
	private PostCollection collection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;
}
