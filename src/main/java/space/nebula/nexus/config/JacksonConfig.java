package space.nebula.nexus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Jackson 2 mapper used by durable message payloads.
 *
 * Spring Boot 4's web stack no longer guarantees a Jackson 2 ObjectMapper bean,
 * while the AMQP and newsletter delivery code still serializes with Jackson 2.
 * Keeping this bean explicit makes the application context deterministic in
 * both the dev container and integration tests.
 */
@Configuration
public class JacksonConfig {

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper().findAndRegisterModules().setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
}
