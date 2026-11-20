package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_tag_translation", uniqueConstraints = @UniqueConstraint(name = "uk_tag_translation_locale", columnNames = {
		"tag_id", "locale"}))
public class TagTranslation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tag_id", nullable = false)
	private Tag tag;

	@Column(nullable = false, length = 10)
	private String locale;

	@Column(nullable = false, length = 50)
	private String name;
}
