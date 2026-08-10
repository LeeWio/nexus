package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostRevisionKind;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "blog_post_revision", uniqueConstraints = @UniqueConstraint(name = "uk_revision_post_version", columnNames = {
		"post_id", "version_number"}))
public class PostRevision implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_revision_id")
	private PostRevision parentRevision;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 500)
	private String summary;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 20)
	private PostContentType contentType = PostContentType.JSON;

	@Column(name = "version_number", nullable = false)
	private Integer versionNumber;

	@Column(name = "base_version_number")
	private Integer baseVersionNumber;

	@Column(name = "source_revision_id")
	private Long sourceRevisionId;

	@Column(name = "change_type", length = 50)
	private String changeType;

	@Enumerated(EnumType.STRING)
	@Column(name = "revision_kind", nullable = false, length = 32)
	private PostRevisionKind revisionKind = PostRevisionKind.LEGACY;

	@Column(name = "change_summary", length = 500)
	private String changeSummary;

	@Column(name = "content_hash", length = 64)
	private String contentHash;

	@Lob
	@Column(name = "snapshot_json", columnDefinition = "LONGTEXT")
	private String snapshotJson;

	@Column(name = "snapshot_hash", length = 64)
	private String snapshotHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
