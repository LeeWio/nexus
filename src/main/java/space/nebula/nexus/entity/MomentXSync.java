package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.MomentXSyncStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_moment_x_sync")
@SQLDelete(sql = "UPDATE blog_moment_x_sync SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class MomentXSync extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "moment_id", nullable = false, unique = true)
	private Moment moment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private MomentXSyncStatus status = MomentXSyncStatus.PENDING;

	@Column(name = "x_post_id", length = 64)
	private String xPostId;

	@Column(name = "x_post_url", length = 255)
	private String xPostUrl;

	@Column(nullable = false)
	private Integer attempts = 0;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@Column(name = "posted_at")
	private LocalDateTime postedAt;
}
