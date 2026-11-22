package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.NewsletterProperties;
import space.nebula.nexus.entity.MomentSubscriber;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.repository.MomentSubscriberRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomentSubscriptionService {
	private static final Duration RESEND_DELAY = Duration.ofMinutes(10);

	private final MomentSubscriberRepository subscribers;
	private final MomentEmailDeliveryService delivery;
	private final NewsletterProperties newsletterProperties;

	@Transactional
	public void subscribe(String address, Locale locale) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String email = address.strip().toLowerCase(Locale.ROOT);
		String language = "zh".equals(locale.getLanguage()) ? "zh" : "en";
		subscribers.ensureSubscriber(email, language, now);
		MomentSubscriber subscriber = subscribers.findByEmail(email).orElseThrow();
		if (subscriber.getStatus() == SubscriberStatus.ACTIVE || recentlyRequested(subscriber, now)) {
			return;
		}

		subscriber.setStatus(SubscriberStatus.PENDING);
		subscriber.setGeneration(Math.incrementExact(subscriber.getGeneration()));
		subscriber.setLanguage(language);
		subscriber.setVerificationToken(UUID.randomUUID().toString());
		subscriber.setVerificationRequestedAt(now);
		subscriber.setVerificationExpiresAt(now.plus(newsletterProperties.getVerificationTtl()));
		subscriber.setUnsubscribeToken(UUID.randomUUID().toString());
		subscriber.setVerifiedAt(null);
		delivery.enqueueConfirmation(subscriber, now);
	}

	@Transactional
	public void verify(String token) {
		MomentSubscriber subscriber = subscribers.findByVerificationToken(token)
				.orElseThrow(MomentSubscriptionService::invalidLink);
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		if (!subscriber.getVerificationExpiresAt().isAfter(now)) {
			throw invalidLink();
		}
		if (subscriber.getStatus() == SubscriberStatus.PENDING) {
			subscriber.setStatus(SubscriberStatus.ACTIVE);
			subscriber.setVerifiedAt(now);
		}
		// Keep the token until expiry so a retried confirmation is idempotent.
	}

	@Transactional
	public void unsubscribe(String token) {
		MomentSubscriber subscriber = subscribers.findByUnsubscribeToken(token)
				.orElseThrow(MomentSubscriptionService::invalidLink);
		subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
		subscriber.setVerificationToken(null);
		subscriber.setVerificationRequestedAt(null);
		subscriber.setVerificationExpiresAt(null);
	}

	private boolean recentlyRequested(MomentSubscriber subscriber, LocalDateTime now) {
		return subscriber.getStatus() == SubscriberStatus.PENDING && subscriber.getVerificationRequestedAt() != null
				&& subscriber.getVerificationExpiresAt().isAfter(now)
				&& subscriber.getVerificationRequestedAt().plus(RESEND_DELAY).isAfter(now);
	}

	private static BusinessException invalidLink() {
		return new BusinessException(BusinessCode.BAD_REQUEST, "Moments subscription link is invalid or expired");
	}
}
