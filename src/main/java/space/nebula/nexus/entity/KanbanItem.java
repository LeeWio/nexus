package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.KanbanPriority;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "kanban_item")
@SQLDelete(sql = "UPDATE kanban_item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class KanbanItem extends BaseEntity {

	@Column(nullable = false, length = 255)
	private String title;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private KanbanPriority priority = KanbanPriority.MEDIUM;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex = 0;

	@Column(name = "reminder_at")
	private LocalDateTime reminderAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "column_id", nullable = false)
	private KanbanColumn column;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "kanban_item_tag", joinColumns = @JoinColumn(name = "item_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();
}
