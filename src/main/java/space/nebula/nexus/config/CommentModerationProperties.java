package space.nebula.nexus.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.comment.moderation")
public class CommentModerationProperties
{
	@Min(1)
	private long autoReviewReportThreshold = 3L;

	@Min(1)
	private long highRiskReportThreshold = 2L;
}
