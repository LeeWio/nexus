package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_webhook")
@SQLDelete(sql = "UPDATE blog_webhook SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Webhook extends BaseEntity
{

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(nullable = false, length = 100)
	private String secret;

	@Column(nullable = false, length = 500)
	private String events; // Comma separated list of WebhookEvent enums

	@Column(nullable = false)
	private Boolean isActive = true;
}
