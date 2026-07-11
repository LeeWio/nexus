package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_post")
@SQLDelete(sql = "UPDATE blog_post SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Post extends BaseEntity
{

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, unique = true, length = 200)
	private String slug;

	@Column(name = "cover_image", length = 255)
	private String coverImage;

	@Column(length = 500)
	private String summary;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 20)
	private PostContentType contentType = PostContentType.JSON;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PostStatus status = PostStatus.DRAFT;

	@Column(name = "is_featured")
	private Boolean isFeatured = false;

	@Column(nullable = false)
	private Long views = 0L;

	@Column(name = "likes_count", nullable = false)
	private Long likesCount = 0L;

	@Column(name = "favorites_count", nullable = false)
	private Long favoritesCount = 0L;

	@Column(name = "published_at")
	private java.time.LocalDateTime publishedAt;

	@Column(name = "review_comment", length = 1000)
	private String reviewComment;

	@Column(name = "reviewed_at")
	private java.time.LocalDateTime reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private User reviewedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "series_id")
	private PostSeries series;

	@Column(name = "series_order")
	private Integer seriesOrder = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Post parent;

	@Column(name = "path", length = 1000)
	private String path;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "blog_post_tag", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();

	// --- Domain Logic ---

	/**
	 * Publishes the post, setting status and publication date.
	 */
	public void publish()
	{
		this.status = PostStatus.PUBLISHED;
		if (this.publishedAt == null)
		{
			this.publishedAt = java.time.LocalDateTime.now();
		}
	}

	/**
	 * Moves the post back to draft status.
	 */
	public void moveToDraft()
	{
		this.status = PostStatus.DRAFT;
	}

	/**
	 * Marks the post as rejected during review.
	 */
	public void reject()
	{
		this.status = PostStatus.REJECTED;
	}

	/**
	 * Checks if the given user is the author of this post.
	 */
	public boolean isAuthor(User user)
	{
		return user != null && this.author != null && user.getId().equals(this.author.getId());
	}

	/**
	 * Checks if the post is currently published.
	 */
	public boolean isPublished()
	{
		return PostStatus.PUBLISHED.equals(this.status);
	}

	/**
	 * Sets the hierarchical path based on the parent post.
	 */
	public void updatePath(Post parent)
	{
		if (this.getId() == null)
		{
			this.path = null;
			return;
		}
		this.path = (parent == null) ? "/" + this.getId() + "/" : parent.getPath() + this.getId() + "/";
	}
}
