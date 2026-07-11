package space.nebula.nexus.config;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures database-backed locks so a scheduled job runs on only one
 * application instance at a time.
 */
@Configuration
public class SchedulerLockConfig {

	@Bean
	LockProvider lockProvider(DataSource dataSource) {
		return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource)).usingDbTime().build());
	}
}
