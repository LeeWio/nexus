package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Type-safe configuration properties for thread pools.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.thread-pool")
@Validated
public class ThreadPoolProperties
{

	/**
	 * Configuration for the general async executor.
	 */
	@Valid
	private AsyncConfig async = new AsyncConfig();

	/**
	 * Configuration for the scheduled task scheduler.
	 */
	@Valid
	private SchedulerConfig scheduler = new SchedulerConfig();

	/** Configuration for outbound HTTP work. */
	@Valid
	private OutboundConfig outbound = new OutboundConfig();

	@Data
	public static class AsyncConfig
	{
		@Min(1)
		private int corePoolSize = 5;
		@Min(1)
		private int maxPoolSize = 10;
		@Min(0)
		private int queueCapacity = 100;
		private String threadNamePrefix = "NexusAsync-";
		@Min(0)
		private int awaitTerminationSeconds = 60;
	}

	@Data
	public static class SchedulerConfig
	{
		@Min(1)
		private int poolSize = 5;
		private String threadNamePrefix = "NexusTask-";
		@Min(0)
		private int awaitTerminationSeconds = 60;
	}

	@Data
	public static class OutboundConfig
	{
		@Min(1)
		private int maxConcurrency = 20;
		private String threadNamePrefix = "NexusOutbound-";
	}
}
