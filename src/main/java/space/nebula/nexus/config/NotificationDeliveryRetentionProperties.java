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
@ConfigurationProperties(prefix = "app.notification-delivery.retention")
@Validated
public class NotificationDeliveryRetentionProperties {
	@Min(1)
	private int deliveryDays = 90;
	@Min(1)
	private int retryAuditDays = 180;
	@Min(1)
	private int batchSize = 200;
}
