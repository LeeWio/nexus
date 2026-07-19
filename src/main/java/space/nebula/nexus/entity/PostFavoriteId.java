package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite identifier for a user's favorite-post relationship.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PostFavoriteId implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Column(name = "post_id")
	private Long postId;

	@Column(name = "user_id")
	private Long userId;
}
