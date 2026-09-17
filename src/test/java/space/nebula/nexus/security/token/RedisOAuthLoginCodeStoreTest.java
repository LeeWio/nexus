package space.nebula.nexus.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.utils.RedisUtil;

@ExtendWith(MockitoExtension.class)
class RedisOAuthLoginCodeStoreTest {

	@Mock
	private RedisUtil redisUtil;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private RedisOAuthLoginCodeStore store;

	@BeforeEach
	void setUp() {
		store = new RedisOAuthLoginCodeStore(redisUtil, objectMapper);
	}

	@Test
	void issueStoresJsonPayloadUnderOpaqueCode() throws Exception {
		AuthResponse authResponse = AuthResponse.builder().accessToken("access").refreshToken("refresh")
				.username("alice").email("alice@example.com").roles(Set.of("ROLE_USER")).build();
		when(redisUtil.set(anyString(), anyString(), eq(RedisOAuthLoginCodeStore.TTL_SECONDS), eq(TimeUnit.SECONDS)))
				.thenReturn(true);

		String code = store.issue(authResponse);

		assertThat(code).isNotBlank().doesNotContain("-");
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(redisUtil).set(keyCaptor.capture(), payloadCaptor.capture(), eq(RedisOAuthLoginCodeStore.TTL_SECONDS),
				eq(TimeUnit.SECONDS));
		assertThat(keyCaptor.getValue()).isEqualTo(RedisOAuthLoginCodeStore.KEY_PREFIX + code);
		assertThat(objectMapper.readValue(payloadCaptor.getValue(), AuthResponse.class)).isEqualTo(authResponse);
	}

	@Test
	void consumeReturnsAuthResponseOnce() throws Exception {
		AuthResponse authResponse = AuthResponse.builder().accessToken("access").refreshToken("refresh")
				.username("alice").roles(Set.of("ROLE_USER")).build();
		String payload = objectMapper.writeValueAsString(authResponse);
		when(redisUtil.getAndDelete(RedisOAuthLoginCodeStore.KEY_PREFIX + "abc123", String.class))
				.thenReturn(Optional.of(payload));

		assertThat(store.consume("abc123")).contains(authResponse);
	}

	@Test
	void consumeReturnsEmptyForMissingCode() {
		when(redisUtil.getAndDelete(RedisOAuthLoginCodeStore.KEY_PREFIX + "missing", String.class))
				.thenReturn(Optional.empty());

		assertThat(store.consume("missing")).isEmpty();
		assertThat(store.consume(" ")).isEmpty();
		assertThat(store.consume(null)).isEmpty();
	}
}
