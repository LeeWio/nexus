package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.exception.BusinessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomentTopicPolicyTest {

	@Test
	void normalizesHashtagsAndPreservesUserSelectionOrder() {
		assertEquals(List.of("frontend-architecture", "上海-summer"),
				MomentTopicPolicy.normalizeTopicSlugs(List.of("#Frontend Architecture", "#上海 Summer")));
	}

	@Test
	void rejectsDuplicatesAfterNormalization() {
		assertThrows(BusinessException.class,
				() -> MomentTopicPolicy.normalizeTopicSlugs(List.of("Frontend Architecture", "frontend-architecture")));
	}

	@Test
	void rejectsMoreThanThreeTopics() {
		assertThrows(BusinessException.class,
				() -> MomentTopicPolicy.normalizeTopicSlugs(List.of("one", "two", "three", "four")));
	}
}
