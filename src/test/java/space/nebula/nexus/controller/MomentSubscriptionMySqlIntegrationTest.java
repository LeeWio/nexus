package space.nebula.nexus.controller;

import com.jayway.jsonpath.JsonPath;
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
import space.nebula.nexus.entity.MomentEmailDelivery.Status;
import space.nebula.nexus.entity.MomentSubscriber;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.MomentEmailDeliveryRepository;
import space.nebula.nexus.repository.MomentSubscriberRepository;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.MomentEmailDeliveryService;
import space.nebula.nexus.service.MomentEmailDeliveryService.Attempt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class}, properties = {
		"app.newsletter.base-url=https://example.test", "app.security.rate-limit.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@EnabledIfSystemProperty(named = "spring.datasource.driver-class-name", matches = "com.mysql.cj.jdbc.Driver")
class MomentSubscriptionMySqlIntegrationTest {
	private static final String SUBSCRIPTIONS = "/api/v1/public/moments/subscription";
	private static final String PUBLIC_MOMENTS = "/api/v1/public/moments";
	private static final String ADMIN_MOMENTS = "/api/v1/admin/moments";
	private static final String ADMIN = "moment-mail-admin";
	private static final String READER = "both@example.test";

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UserRepository users;
	@Autowired
	private MomentSubscriberRepository subscribers;
	@Autowired
	private SubscriberRepository newsletterSubscribers;
	@Autowired
	private MomentEmailDeliveryRepository records;
	@Autowired
	private MomentEmailDeliveryService deliveries;
	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void createAuthor() {
		User author = new User();
		author.setUsername(ADMIN);
		author.setEmail("moment-mail-admin@example.test");
		author.setPassword("fixture");
		author.setStatus(UserStatus.ACTIVE);
		users.saveAndFlush(author);
	}

	@Test
	void audienceUsesFirstPublicReleaseAndOptOutLeavesWeeklyConsentActive() throws Exception {
		jdbc.update("""
				INSERT INTO blog_subscriber (id, email, status, verified_at, created_at, updated_at)
				VALUES (90801, ?, 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP())
				""", READER);

		subscribe(READER);
		subscribe(READER.toUpperCase(java.util.Locale.ROOT));
		assertEquals(1, records.count());
		MomentSubscriber reader = subscriber(READER);
		String verification = reader.getVerificationToken();
		mvc.perform(get(SUBSCRIPTIONS + "/verify").param("token", verification))
				.andExpect(status().isMethodNotAllowed());
		Attempt confirmation = claimOne();
		var confirmationMail = deliveries.prepare(confirmation).orElseThrow();
		assertEquals(READER, confirmationMail.recipient());
		assertEquals("https://example.test/moments/subscription/verify#token=" + verification,
				confirmationMail.variables().get("actionUrl"));
		deliveries.complete(confirmation, true);

		int id = create("private");
		mvc.perform(get(PUBLIC_MOMENTS + "/" + id).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isNotFound());
		assertEquals(1, records.count());
		action("verify", verification);
		var firstVerifiedAt = reader.getVerifiedAt();
		action("verify", verification);
		assertEquals(firstVerifiedAt, reader.getVerifiedAt());

		edit(id, "public");
		edit(id, "public");
		edit(id, "private");
		edit(id, "public");
		mvc.perform(get(PUBLIC_MOMENTS + "/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id));
		assertEquals(2, records.count());

		subscribe("late@example.test");
		action("verify", subscriber("late@example.test").getVerificationToken());
		List<Attempt> claims = deliveries.claimDue(20);
		var publication = claims.stream()
				.filter(attempt -> records.findById(attempt.id()).orElseThrow().getMomentId() != null).findFirst()
				.orElseThrow();
		var publicationMail = deliveries.prepare(publication).orElseThrow();
		assertEquals(READER, publicationMail.recipient());
		assertEquals("https://example.test/moments/entry/" + id, publicationMail.variables().get("actionUrl"));
		assertEquals("List-Unsubscribe=One-Click", publicationMail.headers().get("List-Unsubscribe-Post"));

		mvc.perform(post(SUBSCRIPTIONS + "/one-click").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.queryParam("token", reader.getUnsubscribeToken()).formField("List-Unsubscribe", "One-Click"))
				.andExpect(status().isOk());
		assertTrue(deliveries.prepare(publication).isEmpty());
		deliveries.complete(publication, false);
		assertEquals(Status.CANCELLED, records.findById(publication.id()).orElseThrow().getStatus());
		assertEquals(SubscriberStatus.ACTIVE, newsletterSubscribers.findByEmail(READER).orElseThrow().getStatus());
	}

	@Test
	void retriesRecheckVisibilityAndConsentGeneration() throws Exception {
		subscribe(READER);
		action("verify", subscriber(READER).getVerificationToken());
		deliveries.complete(claimOne(), false);

		int id = create("public");
		Attempt first = claimOne();
		assertFalse(deliveries.prepare(first).isEmpty());
		assertTrue(deliveries.claimDue(20).isEmpty());
		deliveries.fail(first, new IllegalStateException("SMTP fixture failure"));
		assertEquals(Status.FAILED, records.findById(first.id()).orElseThrow().getStatus());
		assertTrue(deliveries.claimDue(20).isEmpty());
		edit(id, "private");
		records.flush();
		jdbc.update("UPDATE blog_moment_email_delivery SET next_attempt_at = '2000-01-01' WHERE id = ?", first.id());
		Attempt second = claimOne();
		assertEquals(2, second.number());
		assertTrue(deliveries.prepare(second).isEmpty());
		deliveries.complete(second, false);

		int deletedId = create("public");
		Attempt deleted = claimOne();
		mvc.perform(delete(ADMIN_MOMENTS + "/" + deletedId).with(user(ADMIN).roles("ADMIN")))
				.andExpect(status().isOk());
		mvc.perform(get(PUBLIC_MOMENTS + "/" + deletedId)).andExpect(status().isNotFound());
		assertTrue(deliveries.prepare(deleted).isEmpty());

		create("public");
		Attempt oldConsent = claimOne();
		action("unsubscribe", subscriber(READER).getUnsubscribeToken());
		subscribe(READER);
		action("verify", subscriber(READER).getVerificationToken());
		assertTrue(deliveries.prepare(oldConsent).isEmpty());
	}

	private void subscribe(String email) throws Exception {
		mvc.perform(post(SUBSCRIPTIONS + "/subscribe").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}")).andExpect(status().isOk());
	}

	private void action(String action, String token) throws Exception {
		mvc.perform(post(SUBSCRIPTIONS + "/" + action).contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\"}")).andExpect(status().isOk());
	}

	private int create(String visibility) throws Exception {
		String response = mvc
				.perform(post(ADMIN_MOMENTS).with(user(ADMIN).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(momentBody(visibility)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.data.id");
	}

	private void edit(int id, String visibility) throws Exception {
		mvc.perform(put(ADMIN_MOMENTS + "/" + id).with(user(ADMIN).roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON).content(momentBody(visibility))).andExpect(status().isOk());
	}

	private String momentBody(String visibility) {
		return "{\"content\":\"A test Moment\",\"visibility\":\"" + visibility + "\"}";
	}

	private MomentSubscriber subscriber(String email) {
		return subscribers.findByEmail(email).orElseThrow();
	}

	private Attempt claimOne() {
		List<Attempt> claims = deliveries.claimDue(20);
		assertEquals(1, claims.size());
		return claims.getFirst();
	}
}
