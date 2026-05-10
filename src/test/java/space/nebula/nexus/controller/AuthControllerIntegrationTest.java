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

@SpringBootTest
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
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // 2. Login
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    public void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Second registration with same username
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk()) // GlobalExceptionHandler returns 200 for BusinessException
                .andExpect(jsonPath("$.code").value(400));
    }
}
