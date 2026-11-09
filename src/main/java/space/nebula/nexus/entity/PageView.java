package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.TrafficClass;

import java.time.LocalDateTime;

/** One accepted first-party page navigation. */
@Getter
@Setter
@Entity
@Table(name = "blog_page_view")
@SQLRestriction("is_deleted = false")
public class PageView extends BaseEntity {

	@Column(name = "viewed_at", nullable = false)
	private LocalDateTime viewedAt;

	@Column(nullable = false, length = 255)
	private String path;

	@Column(length = 160)
	private String title;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "visitor_hash", nullable = false, length = 64)
	private String visitorHash;

	/** Stored for the admin visitor list. Public responses never include it. */
	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(length = 80)
	private String country;

	@Column(length = 80)
	private String region;

	@Column(length = 80)
	private String city;

	@Column(length = 40)
	private String browser;

	@Column(name = "os_name", length = 40)
	private String osName;

	@Column(name = "device_type", length = 16)
	private String deviceType;

	@Column(name = "client_name", length = 40)
	private String clientName;

	@Column(length = 500)
	private String referrer;

	@Column(name = "referrer_host", length = 255)
	private String referrerHost;

	@Column(name = "source_channel", nullable = false, length = 32)
	private String sourceChannel;

	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "traffic_class", nullable = false, length = 16)
	private TrafficClass trafficClass;

	@Column(name = "user_agent", length = 500)
	private String userAgent;
}
