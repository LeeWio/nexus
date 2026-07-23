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
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentStatus;

@Getter
@Setter
@Entity
@Table(name = "blog_comment_moderation_log")
public class CommentModerationLog extends BaseEntity
{

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id", nullable = false)
	private Comment comment;

	@Column(name = "moderator_username", length = 100)
	private String moderatorUsername;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 40)
	private CommentModerationAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", length = 20)
	private CommentStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", length = 20)
	private CommentStatus newStatus;

	@Column(name = "reason", length = 120)
	private String reason;

	@Column(name = "batch_id", length = 36)
	private String batchId;

	@Lob
	@Column(name = "note", columnDefinition = "TEXT")
	private String note;
}
