package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_config")
public class Config extends BaseEntity
{

	@Column(name = "config_key", nullable = false, unique = true, length = 100)
	private String configKey;

	@Lob
	@Column(name = "config_value", columnDefinition = "TEXT")
	private String configValue;

	@Column(name = "config_name", nullable = false, length = 100)
	private String configName;

	@Column(length = 255)
	private String description;

	@Column(name = "is_public", nullable = false)
	private Boolean isPublic = false;
}
