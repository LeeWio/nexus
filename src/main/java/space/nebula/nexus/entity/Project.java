package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_project")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image", length = 255)
    private String coverImage;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "preview_url", length = 255)
    private String previewUrl;

    @Column(name = "tech_stack", length = 255)
    private String techStack;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
}
