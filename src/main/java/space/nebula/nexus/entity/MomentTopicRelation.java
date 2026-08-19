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
 * Ordered association between a Moment and one of its social topics.
 */
@Getter
@Setter
@Entity
@Table(name = "moment_topic_relation", uniqueConstraints = {
		@UniqueConstraint(name = "uk_moment_topic_relation", columnNames = {"moment_id", "topic_id"}),
		@UniqueConstraint(name = "uk_moment_topic_position", columnNames = {"moment_id", "sort_order"})})
public class MomentTopicRelation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "moment_id", nullable = false)
	private Moment moment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private MomentTopic topic;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;
}
