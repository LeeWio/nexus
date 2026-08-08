package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.PasswordResetConfirmRequest;
import space.nebula.nexus.payload.request.PasswordResetRequest;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.utils.RedisUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PasswordResetControllerIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@MockitoBean
	private RedisUtil redisUtil;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		User user = new User();
		user.setUsername("reset-user");
		user.setEmail("reset@example.com");
		user.setPassword(passwordEncoder.encode("CurrentP@ssw0rd1!"));
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}

	@Test
	void requestForUnknownEmailReturnsGenericAcknowledgement() throws Exception {
		PasswordResetRequest request = new PasswordResetRequest("unknown@example.com");

		mockMvc.perform(post("/api/v1/auth/password/reset/request").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.message")
						.value("If an account is associated with this email, password reset instructions have been sent."));
	}

	@Test
	void confirmResetsPasswordAndInvalidatesCurrentTokens() throws Exception {
		PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("reset@example.com", "123456",
				"ResetP@ssw0rd2!");
		when(redisUtil.consumeIfEquals(CacheConstants.PASSWORD_RESET_OTP + request.email(), request.code()))
				.thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/password/reset/confirm").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("Password reset successfully. Please sign in again."));

		User updatedUser = userRepository.findByEmail("reset@example.com").orElseThrow();
		assertTrue(passwordEncoder.matches(request.newPassword(), updatedUser.getPassword()));
		assertEquals(1, updatedUser.getTokenVersion());
	}

	@Test
	void confirmRejectsWeakPasswordsBeforeCheckingTheCode() throws Exception {
		PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("reset@example.com", "123456", "weak");

		mockMvc.perform(post("/api/v1/auth/password/reset/confirm").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40001));
	}
}
