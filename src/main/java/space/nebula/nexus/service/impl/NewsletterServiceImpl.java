package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.config.NewsletterProperties;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.service.IAnalyticsService;
import space.nebula.nexus.service.INewsletterService;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterServiceImpl implements INewsletterService {
	private static final String SUBSCRIPTION_RESPONSE = "Subscription instructions have been sent if the address is eligible.";

	private final SubscriberRepository subscriberRepository;
	private final RabbitTemplate rabbitTemplate;
	private final IAnalyticsService analyticsService;
	private final NewsletterProperties newsletterProperties;

	@Override
	@Transactional
	public ApiResponse<Void> subscribe(String email) {
		Assert.notBlank(email, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Email is required"));
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		var existing = subscriberRepository.findByEmail(normalizedEmail);
		if (existing.isPresent()) {
			if (existing.get().getStatus() == SubscriberStatus.ACTIVE) {
				return ApiResponse.success(SUBSCRIPTION_RESPONSE, null);
			}
			preparePendingSubscription(existing.get());
			subscriberRepository.save(existing.get());
			sendVerificationEmail(existing.get());
		} else {
			Subscriber subscriber = new Subscriber();
			subscriber.setEmail(normalizedEmail);
			preparePendingSubscription(subscriber);
			subscriberRepository.save(subscriber);
			sendVerificationEmail(subscriber);
		}

		return ApiResponse.success(SUBSCRIPTION_RESPONSE, null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> verify(String token) {
		var subscriber = subscriberRepository.findByVerificationToken(token).orElseThrow(
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Verification token is invalid or expired"));

		Assert.isTrue(
				subscriber.getStatus() == SubscriberStatus.PENDING && subscriber.getVerificationExpiresAt() != null
						&& subscriber.getVerificationExpiresAt().isAfter(LocalDateTime.now()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Verification token is invalid or expired"));
		subscriber.setStatus(SubscriberStatus.ACTIVE);
		subscriber.setVerifiedAt(LocalDateTime.now());
		subscriber.setVerificationToken(null);
		subscriber.setVerificationExpiresAt(null);
		subscriberRepository.save(subscriber);

		log.info("New subscriber activated: {}", subscriber.getEmail());
		return ApiResponse.success("Subscription verified successfully.", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> unsubscribe(String token) {
		var subscriber = subscriberRepository.findByUnsubscribeToken(token).orElseThrow(
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Unsubscribe token is invalid or expired"));

		subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
		subscriber.setVerificationToken(null);
		subscriber.setVerificationExpiresAt(null);
		subscriberRepository.save(subscriber);

		log.info("Subscriber opted out: {}", subscriber.getEmail());
		return ApiResponse.success("Unsubscribed successfully.", null);
	}

	@Override
	public void sendWeeklyNewsletter() {
		log.info("Generating weekly newsletter...");
		var trendingResponse = analyticsService.getTrendingPosts(5);
		if (trendingResponse.data() == null || trendingResponse.data().isEmpty()) {
			log.info("No trending content found. Skipping newsletter for this week.");
			return;
		}

		int pageNumber = 0;
		long delivered = 0;
		org.springframework.data.domain.Page<Subscriber> page;
		do {
			page = subscriberRepository.findAllByStatus(SubscriberStatus.ACTIVE,
					PageRequest.of(pageNumber++, newsletterProperties.getBatchSize()));
			for (Subscriber sub : page.getContent()) {
				Map<String, Object> variables = Map.of("posts", trendingResponse.data(), "baseUrl",
						newsletterProperties.getBaseUrl(), "unsubscribeUrl", newsletterProperties.getBaseUrl()
								+ "/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken());

				TemplateMailMessage mail = TemplateMailMessage.builder().to(sub.getEmail())
						.subject("Nexus Weekly: Hot Articles You Might Like").templateName("weekly-newsletter")
						.variables(variables).type(TemplateMailMessage.MailType.TEMPLATE).build();

				try {
					rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mail);
					delivered++;
				} catch (Exception e) {
					log.error("Failed to enqueue newsletter for subscriber id {}", sub.getId(), e);
				}
			}
		} while (page.hasNext());
		log.info("Newsletter broadcast queued for {} subscribers", delivered);
	}

	private void sendVerificationEmail(Subscriber subscriber) {
		String verifyUrl = newsletterProperties.getBaseUrl() + "/newsletter/verify?token="
				+ subscriber.getVerificationToken();

		Map<String, Object> variables = Map.of("verifyUrl", verifyUrl, "email", subscriber.getEmail(), "baseUrl",
				newsletterProperties.getBaseUrl());

		TemplateMailMessage mail = TemplateMailMessage.builder().to(subscriber.getEmail())
				.subject("Verify your Nexus subscription").templateName("newsletter-verify").variables(variables)
				.type(TemplateMailMessage.MailType.TEMPLATE).build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mail);
	}

	private void preparePendingSubscription(Subscriber subscriber) {
		subscriber.setStatus(SubscriberStatus.PENDING);
		subscriber.setVerificationToken(IdUtil.fastSimpleUUID());
		subscriber.setVerificationExpiresAt(LocalDateTime.now().plus(newsletterProperties.getVerificationTtl()));
		// A new consent flow invalidates every unsubscribe link from an older flow.
		subscriber.setUnsubscribeToken(IdUtil.fastSimpleUUID());
	}
}
