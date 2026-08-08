package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.junit.jupiter.api.BeforeEach;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class,
		space.nebula.nexus.config.MockRabbitMQConfig.class})
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@BeforeEach
	public void setup() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
	}

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	public void testRegisterAndLogin() throws Exception {
		// 1. Register
		RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "P@ssw0rd123!");

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Registration successful. Your account is awaiting approval."));

		// 2. Login
		LoginRequest loginRequest = new LoginRequest("testuser", "P@ssw0rd123!");

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Authentication service is temporarily unavailable"));
	}

	@Test
	public void testRegisterDuplicateUsername() throws Exception {
		RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "P@ssw0rd123!");

		// First registration
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isOk());

		// Second registration with same username
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40002));
	}

	@Test
	public void refreshRejectsRequestsWithoutAToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001))
				.andExpect(jsonPath("$.message").value("Validation failed: Refresh token is required"));
	}
}
