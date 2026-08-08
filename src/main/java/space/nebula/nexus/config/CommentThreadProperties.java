package space.nebula.nexus.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Limits for publicly visible comment threads.
 */
@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.comment.thread")
public class CommentThreadProperties {
	/** Number of nested replies allowed below a root comment. */
	@Min(1)
	@Max(10)
	private int maxReplyDepth = 5;
}
