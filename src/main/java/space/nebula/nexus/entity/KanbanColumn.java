package space.nebula.nexus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "kanban_column")
public class KanbanColumn extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 50)
	private String color;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex = 0;

	@OneToMany(mappedBy = "column", cascade = CascadeType.ALL)
	@OrderBy("orderIndex ASC")
	private List<KanbanItem> items = new ArrayList<>();
}
