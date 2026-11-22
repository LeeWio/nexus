package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.NewsletterProperties;
import space.nebula.nexus.entity.MomentEmailDelivery;
import space.nebula.nexus.entity.MomentEmailDelivery.Status;
import space.nebula.nexus.entity.MomentSubscriber;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.repository.MomentEmailDeliveryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.MomentSubscriberRepository;
import space.nebula.nexus.service.support.EmailDeliveryRetry;
import space.nebula.nexus.utils.MomentContentPolicy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MomentEmailDeliveryService {
	private static final List<Status> RETRYABLE = List.of(Status.QUEUED, Status.FAILED, Status.SENDING);
	private static final int SUMMARY_LENGTH = 200;
	private static final String SUBSCRIPTION_PATH = "/moments/subscription/";

	private final MomentEmailDeliveryRepository deliveries;
	private final MomentSubscriberRepository subscribers;
	private final MomentRepository moments;
	private final NewsletterProperties newsletterProperties;

	@Transactional
	public void enqueuePublication(Long momentId, LocalDateTime publishedAt) {
		deliveries.enqueuePublication(momentId, publishedAt);
	}

	@Transactional
	public void enqueueConfirmation(MomentSubscriber subscriber, LocalDateTime now) {
		MomentEmailDelivery delivery = new MomentEmailDelivery();
		delivery.setSubscriberId(subscriber.getId());
		delivery.setGeneration(subscriber.getGeneration());
		delivery.setNextAttemptAt(now);
		deliveries.save(delivery);
	}

	@Transactional
	public List<Attempt> claimDue(int limit) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		List<Attempt> claims = new ArrayList<>();
		for (MomentEmailDelivery delivery : deliveries.findDue(RETRYABLE, now, PageRequest.of(0, limit))) {
			if (delivery.getAttempts() >= EmailDeliveryRetry.MAX_ATTEMPTS) {
				delivery.setStatus(Status.ABANDONED);
				continue;
			}
			delivery.setAttempts(delivery.getAttempts() + 1);
			delivery.setStatus(Status.SENDING);
			delivery.setNextAttemptAt(now.plus(EmailDeliveryRetry.LEASE));
			claims.add(new Attempt(delivery.getId(), delivery.getAttempts()));
		}
		return claims;
	}

	/**
	 * Reads current consent and visibility as close to the SMTP call as possible.
	 */
	@Transactional(readOnly = true)
	public Optional<Mail> prepare(Attempt attempt) {
		return deliveries.findById(attempt.id()).filter(row -> ownsAttempt(row, attempt))
				.flatMap(row -> subscribers.findById(row.getSubscriberId())
						.filter(subscriber -> !Boolean.TRUE.equals(subscriber.getIsDeleted())
								&& subscriber.getGeneration() == row.getGeneration())
						.flatMap(subscriber -> prepare(row, subscriber)));
	}

	@Transactional
	public void complete(Attempt attempt, boolean sent) {
		deliveries.findForUpdate(attempt.id()).filter(row -> ownsAttempt(row, attempt)).ifPresent(row -> {
			row.setStatus(sent ? Status.DELIVERED : Status.CANCELLED);
			row.setLastError(null);
		});
	}

	@Transactional
	public void fail(Attempt attempt, Exception error) {
		deliveries.findForUpdate(attempt.id()).filter(row -> ownsAttempt(row, attempt)).ifPresent(row -> {
			row.setStatus(attempt.number() >= EmailDeliveryRetry.MAX_ATTEMPTS ? Status.ABANDONED : Status.FAILED);
			row.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).plus(EmailDeliveryRetry.backoff(attempt.number())));
			row.setLastError(error.getClass().getSimpleName());
		});
	}

	private Optional<Mail> prepare(MomentEmailDelivery delivery, MomentSubscriber subscriber) {
		String baseUrl = newsletterProperties.getBaseUrl().replaceFirst("/+$", "");
		boolean chinese = "zh".equals(subscriber.getLanguage());
		if (delivery.getMomentId() == null) {
			if (subscriber.getStatus() != SubscriberStatus.PENDING
					|| !subscriber.getVerificationExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
				return Optional.empty();
			}
			String heading = chinese ? "确认订阅动态" : "Confirm your Moments subscription";
			String summary = chinese
					? "确认后，每有一条新的公开动态，你都会收到一封邮件。"
					: "After confirming, you’ll get an email each time a new public Moment is published.";
			return Optional.of(new Mail(subscriber.getEmail(), heading,
					Map.of("heading", heading, "summary", summary, "actionLabel",
							chinese ? "确认订阅" : "Confirm subscription", "actionUrl",
							baseUrl + SUBSCRIPTION_PATH + "verify#token=" + subscriber.getVerificationToken()),
					Map.of()));
		}
		if (subscriber.getStatus() != SubscriberStatus.ACTIVE) {
			return Optional.empty();
		}
		return moments.findByIdAndVisibility(delivery.getMomentId(), MomentVisibility.PUBLIC).map(moment -> {
			String heading = chinese ? "有一条新动态" : "A new Moment";
			String text = MomentContentPolicy.visibleText(moment.getContent()).strip().replaceAll("\\s+", " ");
			String summary = text.isBlank()
					? (chinese ? "打开动态查看照片。" : "Open the Moment to see the photos.")
					: truncateSummary(text);
			String token = subscriber.getUnsubscribeToken();
			Map<String, String> headers = baseUrl.startsWith("https://")
					? Map.of("List-Unsubscribe",
							"<" + baseUrl + "/api/v1/public/moments/subscription/one-click?token=" + token + ">",
							"List-Unsubscribe-Post", "List-Unsubscribe=One-Click")
					: Map.of();
			return new Mail(subscriber.getEmail(), heading,
					Map.of("heading", heading, "summary", summary, "actionLabel", chinese ? "查看动态" : "View Moment",
							"actionUrl", baseUrl + "/moments/entry/" + moment.getId(), "unsubscribeLabel",
							chinese ? "退订动态邮件" : "Unsubscribe from Moments", "unsubscribeUrl",
							baseUrl + SUBSCRIPTION_PATH + "unsubscribe#token=" + token),
					headers);
		});
	}

	private String truncateSummary(String text) {
		int length = text.codePointCount(0, text.length());
		return length <= SUMMARY_LENGTH
				? text
				: text.substring(0, text.offsetByCodePoints(0, SUMMARY_LENGTH - 1)) + "…";
	}

	private boolean ownsAttempt(MomentEmailDelivery delivery, Attempt attempt) {
		return delivery.getStatus() == Status.SENDING && delivery.getAttempts() == attempt.number();
	}

	public record Attempt(Long id, int number) {
	}

	public record Mail(String recipient, String subject, Map<String, Object> variables, Map<String, String> headers) {
	}
}
