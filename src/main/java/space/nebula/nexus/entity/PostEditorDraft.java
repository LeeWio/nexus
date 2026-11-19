package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "blog_post_editor_draft", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "identifier"}))
public class PostEditorDraft {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 19)
	private String identifier;

	@Column(nullable = false, length = 36)
	private String revision;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, Object> content;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "post_data", nullable = false)
	private Map<String, Object> postData;

	@Column(name = "saved_at", nullable = false)
	private LocalDateTime savedAt;
}
