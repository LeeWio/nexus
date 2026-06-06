package space.nebula.nexus.config;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ThreadPoolConfig
{

	private final ThreadPoolProperties properties;

	/**
	 * Custom executor for general asynchronous tasks.
	 */
	@Bean(name = "asyncExecutor")
	Executor asyncExecutor()
	{
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
		executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
		executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
		executor.setThreadNamePrefix(properties.getAsync().getThreadNamePrefix());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(properties.getAsync().getAwaitTerminationSeconds());

		// Transfer MDC context to async threads
		executor.setTaskDecorator(new MdcTaskDecorator());

		executor.initialize();
		return executor;
	}

	/**
	 * Custom scheduler for background scheduled tasks.
	 */
	@Bean(name = "taskScheduler")
	ThreadPoolTaskScheduler taskScheduler()
	{
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(properties.getScheduler().getPoolSize());
		scheduler.setThreadNamePrefix(properties.getScheduler().getThreadNamePrefix());
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(properties.getScheduler().getAwaitTerminationSeconds());
		scheduler.initialize();
		return scheduler;
	}
}
