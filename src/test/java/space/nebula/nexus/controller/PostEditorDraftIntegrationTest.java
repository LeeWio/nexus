package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.PostEditorDraftRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostEditorDraftIntegrationTest {
	private static final String AUTHOR = "draft-author";
	private static final String OTHER = "draft-other";
	private static final String DRAFT = "/api/v1/admin/posts/editor-drafts/new";
	private static final String POST = "/api/v1/admin/posts";
	private static final String DOCUMENT = """
			{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Draft content"}]}]}
			""";

	@MockitoBean
	private PostSearchRepository searchRepository;
	@Autowired
	private MockMvc mvc;
	@Autowired
	private UserRepository users;
	@Autowired
	private PostEditorDraftRepository drafts;
	@Autowired
	private ObjectMapper json;
	private Long userId;

	@BeforeEach
	void createAccounts() {
		for (String username : new String[]{AUTHOR, OTHER}) {
			User account = new User();
			account.setUsername(username);
			account.setEmail(username + "@example.test");
			account.setPassword("fixture-value");
			account.setStatus(UserStatus.ACTIVE);
			User saved = users.saveAndFlush(account);
			if (username.equals(AUTHOR))
				userId = saved.getId();
		}
	}

	@Test
	void storesOnePrivateDraftAndRejectsLateSavesAfterDiscard() throws Exception {
		mvc.perform(get(DRAFT)).andExpect(status().isUnauthorized());
		String older = save(DRAFT, null, "First draft");
		String latest = save(DRAFT, older, "Revised draft");
		assertEquals(latest, save(DRAFT, older, "Revised draft"));
		assertEquals(1, drafts.findAssetReferences("draft-cover.webp", null, Limit.of(1)).size());
		mvc.perform(get(DRAFT).with(user(AUTHOR))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.postData.title").value("Revised draft"))
				.andExpect(jsonPath("$.data.content.type").value("doc"))
				.andExpect(jsonPath("$.data.savedAt").isString());
		mvc.perform(get(DRAFT).with(user(OTHER))).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
		mvc.perform(delete(DRAFT).param("revision", latest).with(user(AUTHOR))).andExpect(status().isOk());
		mvc.perform(put(DRAFT).with(user(AUTHOR)).contentType(MediaType.APPLICATION_JSON)
				.content(payload(older, "Late draft"))).andExpect(status().isConflict());
		mvc.perform(get(DRAFT).with(user(AUTHOR))).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void savingAnArticleConsumesOnlyItsOwnDraft() throws Exception {
		String revision = save(DRAFT, null, "Private draft");
		String payload = json.createObjectNode().put("title", "Editor draft fixture")
				.put("slug", "editor-draft-fixture").put("contentType", "JSON").put("content", DOCUMENT)
				.put("status", "DRAFT").toString();
		String response = mvc
				.perform(post(POST).with(user(AUTHOR).roles("ADMIN")).header("X-Editor-Draft-Revision", revision)
						.contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(drafts.findByUserIdAndIdentifier(userId, "new").isEmpty());
		String postId = json.readTree(response).path("data").path("id").asText();
		String existing = POST + "/editor-drafts/" + postId;
		String existingRevision = save(existing, null, "Updated draft");
		save(DRAFT, null, "Another article");
		mvc.perform(put(POST + "/" + postId).with(user(AUTHOR).roles("ADMIN"))
				.header("X-Editor-Draft-Revision", existingRevision).contentType(MediaType.APPLICATION_JSON)
				.content(payload)).andExpect(status().isOk());
		mvc.perform(get(existing).with(user(AUTHOR).roles("ADMIN"))).andExpect(jsonPath("$.data").isEmpty());
		mvc.perform(get(DRAFT).with(user(AUTHOR)))
				.andExpect(jsonPath("$.data.postData.title").value("Another article"));
	}

	private String save(String path, String revision, String title) throws Exception {
		String response = mvc
				.perform(put(path).with(user(AUTHOR).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(payload(revision, title)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(response).path("data").path("revision").asText();
	}

	private String payload(String revision, String title) throws Exception {
		var body = json.createObjectNode();
		body.set("content", json.readTree(DOCUMENT));
		body.putObject("postData").put("title", title).put("coverImage", "/media/draft-cover.webp");
		return body.put("revision", revision).toString();
	}
}
