package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_friend_link")
public class FriendLink extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(length = 255)
    private String avatar;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;
}
