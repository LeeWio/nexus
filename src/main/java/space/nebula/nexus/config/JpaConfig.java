package space.nebula.nexus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableJpaAuditing
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class JpaConfig
{
}
