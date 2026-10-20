package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_life_list_memory")
@SQLDelete(sql = "UPDATE blog_life_list_memory SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class LifeListMemory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private LifeListItem item;

	@Column(name = "occurred_at")
	private LocalDate occurredAt;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(length = 160)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LifeListVisibility visibility = LifeListVisibility.PRIVATE;

	@Column(name = "blog_post_id")
	private Long blogPostId;
}
