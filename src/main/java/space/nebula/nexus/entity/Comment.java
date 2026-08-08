package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.CommentStatus;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_comment")
@SQLDelete(sql = "UPDATE blog_comment SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Comment extends BaseEntity {

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommentStatus status = CommentStatus.PENDING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id") // Nullable for guestbook
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Comment parent;

	@Column(name = "path", length = 1000)
	private String path;

	@Column(name = "ip_address", length = 50)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "client_request_id", length = 80)
	private String clientRequestId;

	@Column(name = "likes_count", nullable = false)
	private Long likesCount = 0L;

	@Column(name = "reports_count", nullable = false)
	private Long reportsCount = 0L;

	@Column(name = "edited_at")
	private java.time.LocalDateTime editedAt;

	@Column(name = "edit_count", nullable = false)
	private Integer editCount = 0;

	@Column(name = "is_pinned", nullable = false)
	private Boolean pinned = false;

	@Column(name = "is_featured", nullable = false)
	private Boolean featured = false;

	@Column(name = "is_deleted_placeholder", nullable = false)
	private Boolean deletedPlaceholder = false;

	// --- Domain Logic ---

	/**
	 * Approves the comment for public visibility.
	 */
	public void approve() {
		this.status = CommentStatus.APPROVED;
	}

	/**
	 * Rejects the comment.
	 */
	public void reject() {
		this.status = CommentStatus.REJECTED;
	}

	/**
	 * Updates comment content and marks the edit timestamp.
	 */
	public void editContent(String content) {
		this.content = content;
		this.editedAt = java.time.LocalDateTime.now();
		this.editCount = this.editCount == null ? 1 : this.editCount + 1;
	}

	/**
	 * Sets the hierarchical path based on the parent comment.
	 */
	public void updatePath(Comment parent) {
		if (this.getId() == null) {
			throw new IllegalStateException("ID must be set before path generation");
		}
		this.path = (parent == null) ? "/" + this.getId() + "/" : parent.getPath() + this.getId() + "/";
	}

	/**
	 * Checks if the comment is for a specific post.
	 */
	public boolean belongsToPost(Long postId) {
		return this.post != null && this.post.getId().equals(postId);
	}

	public void markDeletedPlaceholder() {
		this.content = "[deleted]";
		this.deletedPlaceholder = true;
		this.pinned = false;
		this.featured = false;
		this.status = CommentStatus.APPROVED;
	}

	/**
	 * Deleted placeholders remain visible only to preserve thread context and must
	 * not accept new public interaction.
	 */
	public boolean isDeletedPlaceholder() {
		return Boolean.TRUE.equals(deletedPlaceholder);
	}
}
