package space.nebula.nexus.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityEndpointIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Test
	void livenessProbeIsPublicAndHidesComponents() throws Exception {
		mockMvc.perform(get("/livez")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@AfterEach
	void restoreReadiness() {
		AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
	}

	@Test
	void readinessProbeReflectsUnavailableApplicationWithoutLeakingComponents() throws Exception {
		AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);

		mockMvc.perform(get("/readyz")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void managementHealthStillRequiresAdministrator() throws Exception {
		mockMvc.perform(get("/management/health")).andExpect(status().isUnauthorized());
	}
}
