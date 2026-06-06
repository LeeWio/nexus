package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.service.IAnalyticsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceImplTest {

    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private IAnalyticsService analyticsService;

    @InjectMocks
    private NewsletterServiceImpl newsletterService;

    @Test
    void subscribe_NewEmail_Success() {
        String email = "test@example.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());

        var response = newsletterService.subscribe(email);

        assertEquals(200, response.code());
        verify(subscriberRepository).save(any(Subscriber.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void verify_ValidToken_Success() {
        String token = "valid-token";
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("test@example.com");
        subscriber.setStatus("PENDING");
        
        when(subscriberRepository.findByVerificationToken(token)).thenReturn(Optional.of(subscriber));

        var response = newsletterService.verify(token);

        assertEquals(200, response.code());
        assertEquals("ACTIVE", subscriber.getStatus());
        verify(subscriberRepository).save(subscriber);
    }

    @Test
    void unsubscribe_ValidToken_Success() {
        String token = "unsub-token";
        Subscriber subscriber = new Subscriber();
        subscriber.setStatus("ACTIVE");
        
        when(subscriberRepository.findByUnsubscribeToken(token)).thenReturn(Optional.of(subscriber));

        var response = newsletterService.unsubscribe(token);

        assertEquals(200, response.code());
        assertEquals("UNSUBSCRIBED", subscriber.getStatus());
        verify(subscriberRepository).save(subscriber);
    }
}
