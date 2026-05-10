package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_moment")
public class Moment extends BaseEntity {

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;
}
