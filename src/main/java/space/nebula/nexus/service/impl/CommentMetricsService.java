package space.nebula.nexus.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentStatus;

@Service
@RequiredArgsConstructor
public class CommentMetricsService {

	private final ObjectProvider<MeterRegistry> meterRegistryProvider;

	public void incrementPublished(CommentStatus status) {
		increment("nexus.comment.publish", "status", status.name());
	}

	public void incrementReport(boolean accepted) {
		increment("nexus.comment.report", "accepted", Boolean.toString(accepted));
	}

	public void incrementModeration(CommentModerationAction action, CommentStatus status) {
		increment("nexus.comment.moderation", "action", action.name(), "status", status.name());
	}

	private void increment(String name, String... tags) {
		MeterRegistry registry = meterRegistryProvider.getIfAvailable();
		if (registry != null) {
			registry.counter(name, tags).increment();
		}
	}
}
