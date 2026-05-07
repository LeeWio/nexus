package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.Collections;
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Long categoryId;

    @BeforeEach
    public void setup() {
        // Create a test user
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("password");
            admin.setEmail("admin@example.com");
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
        }

        // Create a test category
        Category category = new Category();
        category.setName("Test Category");
        category.setSlug("test-category");
        category = categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateAndGetPost() throws Exception {
        PostRequest request = new PostRequest(
                "Test Post Title",
                "test-post-slug",
                null,
                "Summary of test post",
                "Content of test post",
                PostStatus.PUBLISHED,
                false,
                categoryId,
                new HashSet<>()
        );

        // 1. Create Post
        mockMvc.perform(post("/api/v1/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Post Title"))
                .andExpect(jsonPath("$.data.slug").value("test-post-slug"));

        // 2. Get Public Posts
        mockMvc.perform(get("/api/v1/public/blog/posts")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdatePost() throws Exception {
        PostRequest createRequest = new PostRequest(
                "Original Title",
                "original-slug",
                null,
                "Summary",
                "Content",
                PostStatus.DRAFT,
                false,
                categoryId,
                null
        );

        String response = mockMvc.perform(post("/api/v1/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        
        Long postId = objectMapper.readTree(response).get("data").get("id").asLong();

        PostRequest updateRequest = new PostRequest(
                "Updated Title",
                "updated-slug",
                null,
                "Updated Summary",
                "Updated Content",
                PostStatus.PUBLISHED,
                true,
                categoryId,
                null
        );

        mockMvc.perform(put("/api/v1/admin/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.isFeatured").value(true));
    }
}
