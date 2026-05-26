package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_tag")
@SQLDelete(sql = "UPDATE blog_tag SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Tag extends BaseEntity
{

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String slug;
}
