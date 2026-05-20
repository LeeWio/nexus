package space.nebula.nexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import space.nebula.nexus.common.aspect.MdcTaskDecorator;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for Thread Pools to ensure resource isolation.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ThreadPoolConfig {

	/**
	 * Custom executor for general asynchronous tasks.
	 */
	@Bean(name = "asyncExecutor")
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// Core pool size: threads to keep alive
		executor.setCorePoolSize(5);
		// Max pool size: max threads allowed
		executor.setMaxPoolSize(10);
		// Queue capacity: tasks to buffer before creating new threads
		executor.setQueueCapacity(100);
		// Thread name prefix for easier debugging
		executor.setThreadNamePrefix("NexusAsync-");
		// Rejection policy: Run in the caller's thread when saturated
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		// Wait for tasks to complete on shutdown
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);

		// Transfer MDC context to async threads
		executor.setTaskDecorator(new MdcTaskDecorator());

		executor.initialize();
		return executor;
	}

	/**
	 * Custom scheduler for background scheduled tasks.
	 */
	@Bean(name = "taskScheduler")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		// Pool size for scheduled tasks
		scheduler.setPoolSize(5);
		// Thread name prefix
		scheduler.setThreadNamePrefix("NexusTask-");
		// Wait for tasks to complete on shutdown
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(60);
		// Error handler can be added here if needed
		scheduler.initialize();
		return scheduler;
	}
}
