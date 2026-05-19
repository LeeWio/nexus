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
@Table(name = "blog_moment")
@SQLDelete(sql = "UPDATE blog_moment SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Moment extends BaseEntity {

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;
}
