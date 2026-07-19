package space.nebula.nexus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * Configures JPA repositories, auditing, and distributed scheduler locking.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "space.nebula.nexus.repository",
		excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
				pattern = "space\\.nebula\\.nexus\\.repository\\.search\\..*"))
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class JpaConfig
{
}
