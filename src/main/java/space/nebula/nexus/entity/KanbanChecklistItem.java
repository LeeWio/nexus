package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * An ordered checklist entry belonging to a Kanban task.
 */
@Getter
@Setter
@Entity
@Table(name = "kanban_checklist_item")
@SQLDelete(sql = "UPDATE kanban_checklist_item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class KanbanChecklistItem extends BaseEntity {

	@Column(nullable = false, length = 255)
	private String title;

	@Column(nullable = false)
	private Boolean completed = false;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex = 0;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private KanbanItem task;
}
