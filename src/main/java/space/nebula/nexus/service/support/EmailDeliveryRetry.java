package space.nebula.nexus.service.support;

import java.time.Duration;

/** Shared SMTP lease, retry budget and bounded backoff. */
public final class EmailDeliveryRetry {
	public static final int MAX_ATTEMPTS = 5;
	public static final Duration LEASE = Duration.ofMinutes(10);

	private EmailDeliveryRetry() {
	}

	public static Duration backoff(int attempts) {
		return Duration.ofMinutes(Math.min(60, 1L << Math.min(attempts, 6)));
	}
}
