package space.nebula.nexus.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.enums.TrafficClass;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = "spring.test.database.replace=NONE")
@ActiveProfiles("test")
class PageViewRepositoryDataJpaTest {

	@Autowired
	private PageViewRepository repository;
	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void databaseSession() {
		// MySQL tests retain the configured session, including legacy production zones.
		entityManager.unwrap(Session.class).doWork(connection -> {
			if (connection.getMetaData().getDatabaseProductName().equals("H2")) {
				try (var statement = connection.createStatement()) {
					statement.execute("SET TIME ZONE 'UTC'");
				}
			}
		});
	}

	@Test
	void persistsUtcAndReturnsTypedVisitorInstantsWithoutJdbcWallClockConversions() {
		Instant timestamp = Instant.parse("2026-11-16T06:37:00Z");
		PageView view = save(timestamp);
		entityManager.flush();
		entityManager.unwrap(Session.class).doWork(connection -> {
			try (var statement = connection.prepareStatement("SELECT viewed_at FROM blog_page_view WHERE id = ?")) {
				statement.setLong(1, view.getId());
				try (var result = statement.executeQuery()) {
					result.next();
					assertEquals(LocalDateTime.parse("2026-11-16T06:37:00"), result.getObject(1, LocalDateTime.class));
				}
			}
		});
		entityManager.clear();
		assertEquals(timestamp, repository.findById(view.getId()).orElseThrow().getViewedAt());
		var visitors = repository.findVisitors(timestamp.minusSeconds(1), timestamp.plusSeconds(1),
				List.of(TrafficClass.HUMAN), PageRequest.of(0, 50));
		assertEquals(1, visitors.size());
		assertEquals(timestamp, visitors.getFirst().firstSeen());
		assertEquals(timestamp, visitors.getFirst().lastSeen());
		assertEquals(1, visitors.getFirst().views());
	}

	@Test
	void legacyAuditFieldsRetainTheirExistingQuerySemantics() {
		PageView view = save(Instant.parse("2026-11-16T06:37:00Z"));
		entityManager.flush();
		LocalDateTime createdAt = view.getCreatedAt();
		entityManager.clear();
		assertTrue(Duration.between(createdAt, repository.findById(view.getId()).orElseThrow().getCreatedAt()).abs()
				.toNanos() < 1000);
		assertEquals(1L, entityManager.createQuery(
				"SELECT COUNT(v) FROM PageView v WHERE v.id = :id AND v.createdAt >= :start AND v.createdAt < :end",
				Long.class).setParameter("id", view.getId()).setParameter("start", createdAt.minusSeconds(1))
				.setParameter("end", createdAt.plusSeconds(1)).getSingleResult());
	}

	@Test
	void typedVisitorQueryPreservesOwnerAndBotExclusion() {
		Instant timestamp = Instant.parse("2026-11-16T06:37:00Z");
		save(timestamp);
		PageView owner = save(timestamp);
		owner.setTrafficClass(TrafficClass.OWNER);
		PageView bot = save(timestamp);
		bot.setTrafficClass(TrafficClass.BOT);
		entityManager.flush();
		assertEquals(1, repository.findVisitors(timestamp.minusSeconds(1), timestamp.plusSeconds(1),
				List.of(TrafficClass.HUMAN), PageRequest.of(0, 50)).size());
		assertEquals(3,
				repository.findVisitors(timestamp.minusSeconds(1), timestamp.plusSeconds(1),
						List.of(TrafficClass.HUMAN, TrafficClass.OWNER, TrafficClass.BOT), PageRequest.of(0, 50))
						.size());
	}

	@Test
	void readsExistingUtcWallClockRowsAsUtcInstants() {
		PageView view = save(Instant.parse("2026-11-16T00:00:00Z"));
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE blog_page_view SET viewed_at = '2026-11-16 06:37:00' WHERE id = :id")
				.setParameter("id", view.getId()).executeUpdate();
		entityManager.clear();
		assertEquals(Instant.parse("2026-11-16T06:37:00Z"),
				repository.findById(view.getId()).orElseThrow().getViewedAt());
	}

	@Test
	void midnightBelongsOnlyToTheNewShanghaiDayForCountsAndNativeTrends() {
		Instant midnight = Instant.parse("2026-11-15T16:00:00Z");
		save(midnight.minusNanos(1000));
		save(midnight);
		save(midnight.plusSeconds(3600));
		entityManager.flush();
		assertEquals(1, repository.countViews(midnight.minusSeconds(86400), midnight, List.of(TrafficClass.HUMAN)));
		assertEquals(2, repository.countViews(midnight, midnight.plusSeconds(86400), List.of(TrafficClass.HUMAN)));
		assertEquals(2, repository.countVisitorRows(midnight, midnight.plusSeconds(86400), List.of("HUMAN")));
		var trend = repository.findDailyTrend(midnight.minusSeconds(86400), midnight.plusSeconds(86400),
				List.of("HUMAN"), 28800);
		assertEquals(2, trend.size());
		assertEquals("2026-11-15", trend.getFirst().getVisitDate());
		assertEquals(1L, trend.getFirst().getSessions());
		assertEquals("2026-11-16", trend.getLast().getVisitDate());
		assertEquals(2L, trend.getLast().getSessions());
	}

	private PageView save(Instant time) {
		PageView view = new PageView();
		view.setViewedAt(time);
		view.setPath("/moments");
		view.setSessionId(java.util.UUID.randomUUID().toString());
		view.setVisitorHash(java.util.UUID.randomUUID().toString());
		view.setSourceChannel("Direct");
		view.setTrafficClass(TrafficClass.HUMAN);
		view.setCreatedAt(LocalDateTime.parse("2026-11-16T06:37:00"));
		view.setUpdatedAt(LocalDateTime.parse("2026-11-16T06:37:00"));
		return repository.save(view);
	}
}
