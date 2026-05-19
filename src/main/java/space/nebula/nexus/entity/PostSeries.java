package space.nebula.nexus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_series")
@SQLDelete(sql = "UPDATE blog_series SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PostSeries extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

    @OneToMany(mappedBy = "series", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("seriesOrder ASC, createdAt DESC")
    private List<Post> posts = new ArrayList<>();
}
