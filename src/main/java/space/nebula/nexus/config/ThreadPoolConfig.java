package space.nebula.nexus.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import space.nebula.nexus.common.aspect.MdcTaskDecorator;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for Thread Pools to ensure resource isolation. Optimized for
 * Java 21 Virtual Threads.
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig {

	private final ThreadPoolProperties properties;

	@Value("${spring.threads.virtual.enabled:false}")
	private boolean virtualThreadsEnabled;

	/**
	 * Custom executor for general asynchronous tasks. If virtual threads are
	 * enabled, use a virtual-thread-per-task executor.
	 */
	@Bean(name = "asyncExecutor")
	Executor asyncExecutor() {
		if (virtualThreadsEnabled) {
			log.info("Virtual Threads enabled: Using Virtual Thread Per Task Executor for 'asyncExecutor'");
			SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
			executor.setThreadNamePrefix(properties.getAsync().getThreadNamePrefix());
			executor.setVirtualThreads(true);
			executor.setTaskDecorator(new MdcTaskDecorator());
			return executor;
		}

		log.info("Virtual Threads disabled: Using ThreadPoolTaskExecutor for 'asyncExecutor'");
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
		executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
		executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
		executor.setThreadNamePrefix(properties.getAsync().getThreadNamePrefix());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(properties.getAsync().getAwaitTerminationSeconds());
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.initialize();
		return executor;
	}

	/**
	 * Executor dedicated to outbound network calls. The concurrency limit protects
	 * connection pools and remote services even when virtual threads are enabled.
	 */
	@Bean(name = "outboundExecutor")
	Executor outboundExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix(properties.getOutbound().getThreadNamePrefix());
		executor.setVirtualThreads(virtualThreadsEnabled);
		executor.setConcurrencyLimit(properties.getOutbound().getMaxConcurrency());
		executor.setTaskDecorator(new MdcTaskDecorator());
		return executor;
	}

	/**
	 * Custom scheduler for background scheduled tasks. Note:
	 * ThreadPoolTaskScheduler currently doesn't natively support virtual threads in
	 * a simple way like SimpleAsyncTaskExecutor, but for scheduled tasks, platform
	 * threads are usually fine as they are mostly low-concurrency.
	 */
	@Bean(name = "taskScheduler")
	ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(properties.getScheduler().getPoolSize());
		scheduler.setThreadNamePrefix(properties.getScheduler().getThreadNamePrefix());
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(properties.getScheduler().getAwaitTerminationSeconds());
		scheduler.initialize();
		return scheduler;
	}
}
