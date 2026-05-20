package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.FriendLinkStatus;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "blog_friend_link")
@SQLDelete(sql = "UPDATE blog_friend_link SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class FriendLink extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String url;

	@Column(length = 255)
	private String avatar;

	@Column(length = 500)
	private String description;

	@Column(length = 100)
	private String email;

	@Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FriendLinkStatus status = FriendLinkStatus.APPROVED;

	@Column(name = "sort_order")
	private Integer sortOrder = 0;

	@Column(name = "is_published", nullable = false)
	private Boolean isPublished = true;
}
