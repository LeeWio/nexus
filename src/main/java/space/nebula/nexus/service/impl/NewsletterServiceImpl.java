package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.service.IAnalyticsService;
import space.nebula.nexus.service.INewsletterService;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterServiceImpl implements INewsletterService {

    private final SubscriberRepository subscriberRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IAnalyticsService analyticsService;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public ApiResponse<Void> subscribe(String email) {
        Assert.notBlank(email, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Email is required"));
        
        var existing = subscriberRepository.findByEmail(email);
        if (existing.isPresent()) {
            if ("ACTIVE".equals(existing.get().getStatus())) {
                return ApiResponse.success("You are already subscribed", null);
            }
            // If pending or unsubscribed, reset tokens and status
            existing.get().setStatus("PENDING");
            existing.get().setVerificationToken(IdUtil.fastSimpleUUID());
            subscriberRepository.save(existing.get());
            sendVerificationEmail(existing.get());
        } else {
            Subscriber subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriber.setStatus("PENDING");
            subscriber.setVerificationToken(IdUtil.fastSimpleUUID());
            subscriber.setUnsubscribeToken(IdUtil.fastSimpleUUID());
            subscriberRepository.save(subscriber);
            sendVerificationEmail(subscriber);
        }

        return ApiResponse.success("Subscription requested. Please check your inbox for verification.", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> verify(String token) {
        var subscriber = subscriberRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException(BusinessCode.BAD_REQUEST, "Invalid or expired verification token"));
        
        subscriber.setStatus("ACTIVE");
        subscriber.setVerificationToken(null);
        subscriberRepository.save(subscriber);
        
        log.info("New subscriber activated: {}", subscriber.getEmail());
        return ApiResponse.success("Subscription verified successfully. Welcome to our newsletter!", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> unsubscribe(String token) {
        var subscriber = subscriberRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new BusinessException(BusinessCode.BAD_REQUEST, "Invalid unsubscribe token"));
        
        subscriber.setStatus("UNSUBSCRIBED");
        subscriberRepository.save(subscriber);
        
        log.info("Subscriber opted out: {}", subscriber.getEmail());
        return ApiResponse.success("You have been successfully unsubscribed", null);
    }

    @Override
    public void sendWeeklyNewsletter() {
        log.info("Generating weekly newsletter...");
        var trendingResponse = analyticsService.getTrendingPosts(5);
        if (trendingResponse.data() == null || trendingResponse.data().isEmpty()) {
            log.info("No trending content found. Skipping newsletter for this week.");
            return;
        }

        List<Subscriber> activeSubscribers = subscriberRepository.findAllByStatus("ACTIVE");
        log.info("Broadcasting newsletter to {} active subscribers", activeSubscribers.size());

        for (Subscriber sub : activeSubscribers) {
            Map<String, Object> variables = Map.of(
                "posts", trendingResponse.data(),
                "unsubscribeUrl", baseUrl + "/api/v1/public/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken()
            );

            TemplateMailMessage mail = TemplateMailMessage.builder()
                .to(sub.getEmail())
                .subject("Nexus Weekly: Hot Articles You Might Like")
                .templateName("weekly-newsletter")
                .variables(variables)
                .type(TemplateMailMessage.MailType.TEMPLATE)
                .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mail);
        }
    }

    private void sendVerificationEmail(Subscriber subscriber) {
        String verifyUrl = baseUrl + "/api/v1/public/newsletter/verify?token=" + subscriber.getVerificationToken();
        
        Map<String, Object> variables = Map.of(
            "verifyUrl", verifyUrl,
            "email", subscriber.getEmail()
        );

        TemplateMailMessage mail = TemplateMailMessage.builder()
            .to(subscriber.getEmail())
            .subject("Verify your Nexus subscription")
            .templateName("otp-login") // Using otp-login as a base placeholder, should create a custom one later
            .variables(Map.of("otp", "Verify", "message", "Please verify your subscription by clicking: " + verifyUrl))
            .type(TemplateMailMessage.MailType.TEMPLATE)
            .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mail);
    }
}
