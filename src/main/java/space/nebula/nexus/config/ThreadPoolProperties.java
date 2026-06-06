package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for thread pools.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.thread-pool")
public class ThreadPoolProperties
{

	/**
	 * Configuration for the general async executor.
	 */
	private AsyncConfig async = new AsyncConfig();

	/**
	 * Configuration for the scheduled task scheduler.
	 */
	private SchedulerConfig scheduler = new SchedulerConfig();

	@Data
	public static class AsyncConfig
	{
		private int corePoolSize = 5;
		private int maxPoolSize = 10;
		private int queueCapacity = 100;
		private String threadNamePrefix = "NexusAsync-";
		private int awaitTerminationSeconds = 60;
	}

	@Data
	public static class SchedulerConfig
	{
		private int poolSize = 5;
		private String threadNamePrefix = "NexusTask-";
		private int awaitTerminationSeconds = 60;
	}
}
