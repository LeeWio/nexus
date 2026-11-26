package space.nebula.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.LifeListCategory;
import space.nebula.nexus.entity.LifeListItem;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;
import space.nebula.nexus.repository.LifeListCategoryRepository;
import space.nebula.nexus.repository.LifeListItemRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PublicLifeListControllerIntegrationTest {

	private static final String PATH = "/api/v1/public/life-list";
	private static final String DAILY = "fixture-daily";
	private static final String TRAVEL = "fixture-travel";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LifeListCategoryRepository categories;

	@Autowired
	private LifeListItemRepository items;

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@BeforeEach
	void createWishes() {
		LifeListCategory daily = category(DAILY);
		category(TRAVEL);
		for (LifeListStatus status : LifeListStatus.values()) {
			wish(daily, status, LifeListVisibility.PUBLIC, false);
		}
		wish(daily, LifeListStatus.PLANNED, LifeListVisibility.PRIVATE, false);
		wish(daily, LifeListStatus.PLANNED, LifeListVisibility.PUBLIC, true);
	}

	@ParameterizedTest
	@EnumSource(LifeListStatus.class)
	void acceptsAllWireStatuses(LifeListStatus wishStatus) throws Exception {
		mockMvc.perform(get(PATH).param("category", DAILY).param("status", wishStatus.getValue()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.list[0].status").value(wishStatus.getValue()));
	}

	@Test
	void keepsEnumNamesAndReturnsEmptyForUnmatchedFilters() throws Exception {
		mockMvc.perform(get(PATH).param("category", DAILY).param("status", "PLANNED")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(1));
		mockMvc.perform(get(PATH).param("category", TRAVEL).param("status", "completed")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list").isEmpty());
		mockMvc.perform(get(PATH).param("category", DAILY).param("status", "")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(4));
	}

	@Test
	void rejectsInvalidStatusWithBadRequest() throws Exception {
		mockMvc.perform(get(PATH).param("status", "unknown")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
	}

	private LifeListCategory category(String slug) {
		LifeListCategory category = new LifeListCategory();
		category.setName(slug);
		category.setSlug(slug);
		return categories.save(category);
	}

	private void wish(LifeListCategory category, LifeListStatus status, LifeListVisibility visibility,
			boolean archived) {
		LifeListItem item = new LifeListItem();
		item.setNumber(items.findMaxNumber() + 1);
		item.setTitle(status.getValue());
		item.setCategory(category);
		item.setStatus(status);
		item.setVisibility(visibility);
		if (archived) {
			item.setArchivedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		}
		items.save(item);
	}
}
