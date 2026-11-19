package space.nebula.nexus.listener;

import cn.hutool.core.lang.Dict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.payload.response.WebhookLogResponse;
import space.nebula.nexus.service.WebhookAttempts;
import space.nebula.nexus.service.WebhookDelivery;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookMessageListenerTest {

	private static final String ID = "delivery-1";
	private static final String EVENT = "POST_PUBLISHED";
	private static final String PAYLOAD = "{\"postId\":123}";

	@Mock
	private WebhookAttempts attempts;
	@Mock
	private WebhookDelivery delivery;
	@InjectMocks
	private WebhookMessageListener listener;

	@Test
	void brokerRetriesReceiverFailureAfterResultRecorded() {
		WebhookMessage message = new WebhookMessage(1L, ID, EVENT, Dict.create().set("postId", 123));
		var attempt = new WebhookAttempts.Attempt(ID, 1, "https://example.test/webhook", "fixture", EVENT, PAYLOAD);
		when(attempts.claimDispatch(1L, ID, EVENT, PAYLOAD)).thenReturn(Optional.of(attempt));
		when(delivery.send(attempt))
				.thenReturn(new WebhookLogResponse(ID, EVENT, 502, false, 1, LocalDateTime.now(), false));
		assertThrows(IllegalStateException.class, () -> listener.handleWebhookDispatch(message));
	}

	@Test
	void skipsDispatchWhenAttemptCannotBeClaimed() {
		WebhookMessage message = new WebhookMessage(1L, ID, EVENT, Dict.create().set("postId", 123));
		when(attempts.claimDispatch(1L, ID, EVENT, PAYLOAD)).thenReturn(Optional.empty());
		listener.handleWebhookDispatch(message);
		verifyNoInteractions(delivery);
	}
}
