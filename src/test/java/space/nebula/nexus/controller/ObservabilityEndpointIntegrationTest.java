package space.nebula.nexus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityEndpointIntegrationTest
{
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Test
	void livenessProbeIsPublicAndHidesComponents() throws Exception
	{
		mockMvc.perform(get("/livez")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void managementHealthStillRequiresAdministrator() throws Exception
	{
		mockMvc.perform(get("/management/health")).andExpect(status().isUnauthorized());
	}
}
