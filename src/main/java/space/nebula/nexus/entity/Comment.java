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
public class Comment extends BaseEntity
{

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

	// --- Domain Logic ---

	/**
	 * Approves the comment for public visibility.
	 */
	public void approve()
	{
		this.status = CommentStatus.APPROVED;
	}

	/**
	 * Rejects the comment.
	 */
	public void reject()
	{
		this.status = CommentStatus.REJECTED;
	}

	/**
	 * Sets the hierarchical path based on the parent comment.
	 */
	public void updatePath(Comment parent)
	{
		if (this.getId() == null)
		{
			throw new IllegalStateException("ID must be set before path generation");
		}
		this.path = (parent == null) ? "/" + this.getId() + "/" : parent.getPath() + this.getId() + "/";
	}

	/**
	 * Checks if the comment is for a specific post.
	 */
	public boolean belongsToPost(Long postId)
	{
		return this.post != null && this.post.getId().equals(postId);
	}
}
