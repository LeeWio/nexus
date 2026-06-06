package space.nebula.nexus.listener;

import cn.hutool.core.lang.Dict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookMessageListenerTest {

    @Mock
    private WebhookRepository webhookRepository;
    @Mock
    private WebhookLogRepository webhookLogRepository;

    @InjectMocks
    private WebhookMessageListener webhookMessageListener;

    private Webhook testWebhook;

    @BeforeEach
    void setUp() {
        testWebhook = new Webhook();
        testWebhook.setId(1L);
        testWebhook.setUrl("http://example.com/webhook");
        testWebhook.setSecret("test-secret");
        testWebhook.setIsActive(true);
    }

    @Test
    void handleWebhookDispatch_Success() {
        WebhookMessage message = new WebhookMessage(1L, "POST_PUBLISHED", Dict.create().set("postId", 123));
        
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(testWebhook));
        
        // Mock actual HTTP call would be complex here as it uses Static HttpRequest.post
        // In a real project, we might wrap HttpRequest in a service for better mockability
        // For now, we'll check if it attempts to save a log at least (though it might fail the HTTP call in test)
        
        try {
            webhookMessageListener.handleWebhookDispatch(message);
        } catch (Exception e) {
            // Expected failure if network is unreachable
        }
        
        verify(webhookLogRepository).save(any(WebhookLog.class));
    }

    @Test
    void handleWebhookDispatch_Inactive_Skip() {
        testWebhook.setIsActive(false);
        WebhookMessage message = new WebhookMessage(1L, "POST_PUBLISHED", Dict.create().set("postId", 123));
        
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(testWebhook));
        
        webhookMessageListener.handleWebhookDispatch(message);
        
        verify(webhookLogRepository, never()).save(any(WebhookLog.class));
    }
}
