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

	@Column(name = "auto_summary", length = 500)
	private String autoSummary;

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

	@Column(name = "word_count", nullable = false)
	private Integer wordCount = 0;

	@Column(name = "reading_time_minutes", nullable = false)
	private Integer readingTimeMinutes = 1;

	@Column(name = "content_hash", length = 64)
	private String contentHash;

	@Lob
	@Column(name = "toc", columnDefinition = "TEXT")
	private String toc;

	@Column(name = "published_at")
	private java.time.LocalDateTime publishedAt;

	@Column(name = "scheduled_at")
	private java.time.LocalDateTime scheduledAt;

	@Column(name = "review_comment", length = 1000)
	private String reviewComment;

	@Column(name = "reviewed_at")
	private java.time.LocalDateTime reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private User reviewedBy;

	@Column(name = "archive_reason", length = 1000)
	private String archiveReason;

	@Column(name = "archived_at")
	private java.time.LocalDateTime archivedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "archived_by")
	private User archivedBy;

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
		this.scheduledAt = null;
		if (this.publishedAt == null)
		{
			this.publishedAt = java.time.LocalDateTime.now();
		}
	}

	/**
	 * Approves the post for publication at the given future time.
	 *
	 * @param scheduledAt planned publication time
	 */
	public void schedule(java.time.LocalDateTime scheduledAt)
	{
		this.status = PostStatus.SCHEDULED;
		this.scheduledAt = scheduledAt;
		this.publishedAt = null;
	}

	/**
	 * Cancels an approved publication schedule and returns the post to editorial
	 * review.
	 */
	public void cancelSchedule()
	{
		this.status = PostStatus.PENDING_REVIEW;
		this.scheduledAt = null;
	}

	/**
	 * Withdraws the post from editorial review so the author can revise it.
	 */
	public void withdrawFromReview()
	{
		this.status = PostStatus.DRAFT;
		this.reviewComment = null;
		this.reviewedAt = null;
		this.reviewedBy = null;
	}

	/**
	 * Removes a published post from public visibility while retaining its
	 * publication history.
	 *
	 * @param reason editorial archive reason
	 * @param archivedBy editor who archived the post
	 */
	public void archive(String reason, User archivedBy)
	{
		this.status = PostStatus.ARCHIVED;
		this.archiveReason = reason;
		this.archivedAt = java.time.LocalDateTime.now();
		this.archivedBy = archivedBy;
		this.isFeatured = false;
	}

	/**
	 * Restores an archived post as a draft that must pass review before it can be
	 * published again.
	 */
	public void restoreToDraft()
	{
		this.status = PostStatus.DRAFT;
		this.publishedAt = null;
		this.archiveReason = null;
		this.archivedAt = null;
		this.archivedBy = null;
		this.reviewComment = null;
		this.reviewedAt = null;
		this.reviewedBy = null;
	}

	/**
	 * Checks whether content fields may be changed through the standard edit
	 * command.
	 *
	 * @return {@code true} for draft or rejected posts
	 */
	public boolean isEditable()
	{
		return PostStatus.DRAFT.equals(this.status) || PostStatus.REJECTED.equals(this.status);
	}

	/**
	 * Checks whether the post can be deleted without bypassing its active workflow.
	 *
	 * @return {@code true} for draft, rejected, or archived posts
	 */
	public boolean isDeletable()
	{
		return PostStatus.DRAFT.equals(this.status) || PostStatus.REJECTED.equals(this.status)
				|| PostStatus.ARCHIVED.equals(this.status);
	}

	/**
	 * Moves the post back to draft status.
	 */
	public void moveToDraft()
	{
		this.status = PostStatus.DRAFT;
		this.scheduledAt = null;
	}

	/**
	 * Marks the post as rejected during review.
	 */
	public void reject()
	{
		this.status = PostStatus.REJECTED;
		this.scheduledAt = null;
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
