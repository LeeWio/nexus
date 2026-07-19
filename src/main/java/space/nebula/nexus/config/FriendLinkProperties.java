package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Operational settings for friend-link moderation. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.friend-link")
public class FriendLinkProperties {

	/** Inbox that receives new application notifications. */
	private String moderationEmail;
}
