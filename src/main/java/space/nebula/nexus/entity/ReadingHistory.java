package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores the latest reading position for one user and one post.
 */
@Getter
@Setter
@Entity
@Table(name = "blog_reading_history", uniqueConstraints = @UniqueConstraint(name = "uk_reading_history_user_post", columnNames = {
		"user_id", "post_id"}))
public class ReadingHistory extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(name = "progress_percent", nullable = false)
	private Integer progressPercent = 0;

	@Column(name = "position_anchor", length = 500)
	private String positionAnchor;

	@Column(name = "last_read_at", nullable = false)
	private LocalDateTime lastReadAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	/**
	 * Updates the resumable reading state and completion timestamp.
	 *
	 * @param progressPercent
	 *            progress percentage from 0 through 100
	 * @param positionAnchor
	 *            frontend-defined stable reading position
	 */
	public void recordProgress(Integer progressPercent, String positionAnchor) {
		this.progressPercent = progressPercent;
		this.positionAnchor = positionAnchor;
		this.lastReadAt = LocalDateTime.now();
		this.completedAt = progressPercent == 100 ? this.lastReadAt : null;
	}
}
