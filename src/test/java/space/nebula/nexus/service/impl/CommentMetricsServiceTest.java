package space.nebula.nexus.service.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentMetricsServiceTest
{

	@Test
	void incrementsCommentMetricsWithStableTags()
	{
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		@SuppressWarnings("unchecked")
		ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(registry);
		CommentMetricsService service = new CommentMetricsService(provider);

		service.incrementPublished(CommentStatus.APPROVED);
		service.incrementReport(true);
		service.incrementModeration(CommentModerationAction.STATUS_CHANGED, CommentStatus.SPAM);

		assertEquals(1.0, registry.counter("nexus.comment.publish", "status", "APPROVED").count());
		assertEquals(1.0, registry.counter("nexus.comment.report", "accepted", "true").count());
		assertEquals(1.0, registry.counter("nexus.comment.moderation", "action", "STATUS_CHANGED", "status", "SPAM")
			.count());
	}

	@Test
	void ignoresMetricsWhenRegistryIsUnavailable()
	{
		@SuppressWarnings("unchecked")
		ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(null);
		CommentMetricsService service = new CommentMetricsService(provider);

		service.incrementPublished(CommentStatus.PENDING);
		service.incrementReport(false);
		service.incrementModeration(CommentModerationAction.DELETED, CommentStatus.REJECTED);
	}
}
