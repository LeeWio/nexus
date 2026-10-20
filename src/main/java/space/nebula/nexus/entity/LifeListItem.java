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
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_life_list_item")
@SQLDelete(sql = "UPDATE blog_life_list_item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class LifeListItem extends BaseEntity {

	@Column(name = "item_number", nullable = false, unique = true)
	private Integer number;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String motivation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private LifeListCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LifeListStatus status = LifeListStatus.PLANNED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LifeListVisibility visibility = LifeListVisibility.PUBLIC;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder = 0;

	@Column(name = "completed_at")
	private LocalDate completedAt;

	@Column(name = "archived_at")
	private LocalDateTime archivedAt;
}
