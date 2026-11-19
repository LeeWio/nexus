package space.nebula.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@EnabledIfSystemProperty(named = "spring.datasource.driver-class-name", matches = "com.mysql.cj.jdbc.Driver")
class MomentBookmarkMySqlIntegrationTest {

	private static final String SAVED = "/api/v1/user/moments/saved";
	private static final String READER = "bookmark-reader";
	private static final String OTHER = "bookmark-other";
	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UserRepository users;
	@Autowired
	private MomentRepository moments;
	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void createAccounts() {
		for (String username : new String[]{READER, OTHER}) {
			User user = new User();
			user.setUsername(username);
			user.setEmail(username + "@example.test");
			user.setPassword("test");
			user.setStatus(UserStatus.ACTIVE);
			users.saveAndFlush(user);
		}
	}

	@Test
	void savesInAccountOrderAndRetainsRemovalThroughRepeatedImports() throws Exception {
		Long first = moment(MomentVisibility.PUBLIC, OTHER);
		Long second = moment(MomentVisibility.PUBLIC, OTHER);
		mvc.perform(get(SAVED)).andExpect(status().isUnauthorized());
		mvc.perform(put(SAVED + "/" + first).with(user(READER))).andExpect(status().isOk());
		mvc.perform(put(SAVED + "/" + second).with(user(READER))).andExpect(status().isOk());
		mvc.perform(put(SAVED + "/" + first).with(user(READER))).andExpect(status().isOk());
		mvc.perform(get(SAVED).with(user(READER)).param("size", "1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(2))
				.andExpect(jsonPath("$.data.list[0].id").value(second.intValue()));
		mvc.perform(get(SAVED).with(user(READER)).param("size", "1").param("page", "1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].id").value(first.intValue()));
		mvc.perform(delete(SAVED + "/" + second).with(user(OTHER))).andExpect(status().isOk());
		mvc.perform(get(SAVED + "/ids").with(user(OTHER))).andExpect(jsonPath("$.data").isEmpty());
		for (int attempt = 0; attempt < 2; attempt++) {
			mvc.perform(delete(SAVED + "/" + first).with(user(READER))).andExpect(status().isOk());
			importBookmarks(READER, first + "," + second + "," + first);
		}
		mvc.perform(get(SAVED + "/ids").with(user(READER))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0]").value(second.intValue()));
		mvc.perform(put(SAVED + "/" + first).with(user(READER))).andExpect(status().isOk());
		mvc.perform(get(SAVED).with(user(READER))).andExpect(jsonPath("$.data.list[0].id").value(first.intValue()));
	}

	@Test
	void skipsUnavailableMomentsAndRechecksVisibilityWhenReading() throws Exception {
		Long visible = moment(MomentVisibility.PUBLIC, OTHER);
		Long hidden = moment(MomentVisibility.PRIVATE, OTHER);
		Long own = moment(MomentVisibility.PRIVATE, READER);
		Long deleted = moment(MomentVisibility.PUBLIC, OTHER);
		jdbc.update("UPDATE blog_moment SET is_deleted = true WHERE id = ?", deleted);
		importBookmarks(READER, visible + "," + hidden + "," + own + "," + deleted + ",999999999");
		mvc.perform(put(SAVED + "/" + hidden).with(user(READER))).andExpect(status().isNotFound());
		mvc.perform(get(SAVED).with(user(READER))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(2));
		jdbc.update("UPDATE blog_moment SET visibility = 'PRIVATE' WHERE id = ?", visible);
		mvc.perform(get(SAVED + "/ids").with(user(READER))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0]").value(own.intValue()));
		mvc.perform(get(SAVED).with(user(READER))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.list[0].id").value(own.intValue()));
		mvc.perform(post(SAVED + "/import").with(user(READER)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":[0,null]}")).andExpect(status().isBadRequest());
	}

	private void importBookmarks(String username, String ids) throws Exception {
		mvc.perform(post(SAVED + "/import").with(user(username)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":[" + ids + "]}")).andExpect(status().isOk());
	}

	private Long moment(MomentVisibility visibility, String author) {
		Moment moment = new Moment();
		moment.setContent("Bookmark integration fixture");
		moment.setVisibility(visibility);
		Long id = moments.saveAndFlush(moment).getId();
		jdbc.update("UPDATE blog_moment SET created_by = ? WHERE id = ?", author, id);
		return id;
	}
}
