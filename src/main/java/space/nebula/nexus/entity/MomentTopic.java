package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A user-created social topic that is scoped to Moments, not to article
 * taxonomy.
 */
@Getter
@Setter
@Entity
@Table(name = "moment_topic")
public class MomentTopic extends BaseEntity {

	@Column(nullable = false, unique = true, length = 80)
	private String slug;
}
