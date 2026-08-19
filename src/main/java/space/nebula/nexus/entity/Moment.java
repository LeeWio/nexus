package space.nebula.nexus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.MomentVisibility;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_moment")
@SQLDelete(sql = "UPDATE blog_moment SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Moment extends BaseEntity {

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "likes_count")
	private Long likesCount = 0L;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 20)
	private MomentVisibility visibility = MomentVisibility.PUBLIC;

	@OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
	@org.hibernate.annotations.BatchSize(size = 50)
	private List<MomentMedia> images = new ArrayList<>();

	@OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
	@org.hibernate.annotations.BatchSize(size = 50)
	private List<MomentTopicRelation> topicRelations = new ArrayList<>();
}
