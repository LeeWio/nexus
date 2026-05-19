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
@Table(name = "blog_project")
@SQLDelete(sql = "UPDATE blog_project SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
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

    @Column(name = "stars_count")
    private Integer starsCount = 0;

    @Column(name = "forks_count")
    private Integer forksCount = 0;

    @Column(length = 50)
    private String language;

    @Column(name = "repo_name", length = 100)
    private String repoName;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
}
