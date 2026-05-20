package space.nebula.nexus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.UserStatus;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "sys_user")
@SQLDelete(sql = "UPDATE sys_user SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

	@Column(unique = true, nullable = false, length = 50)
	private String username;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(length = 100)
	private String email;

	@Column(length = 50)
	private String nickname;

	@Column(length = 255)
	private String avatar;

	@Column(length = 500)
	private String bio;

	@Column(length = 100)
	private String website;

	@Column(length = 100)
	private String location;

	@Column(name = "github_id", length = 100, unique = true)
	private String githubId;

	@Column(name = "github_username", length = 100)
	private String githubUsername;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name = "sys_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();
}
