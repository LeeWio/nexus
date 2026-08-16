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

@Getter
@Setter
@Entity
@Table(name = "blog_moment_media", uniqueConstraints = {
		@UniqueConstraint(name = "uk_moment_media_position", columnNames = {"moment_id", "sort_order"}),
		@UniqueConstraint(name = "uk_moment_media_file", columnNames = {"moment_id", "file_id"})})
public class MomentMedia extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "moment_id", nullable = false)
	private Moment moment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "file_id", nullable = false)
	private FileMetadata file;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "alt_text", nullable = false, length = 300)
	private String altText;
}
