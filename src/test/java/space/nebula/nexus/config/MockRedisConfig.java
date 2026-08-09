package space.nebula.nexus.config;

import java.util.Properties;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.connection.RedisSetCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisKeyCommands;

@TestConfiguration
public class MockRedisConfig {

	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		RedisConnectionFactory factory = Mockito.mock(RedisConnectionFactory.class);
		RedisConnection connection = Mockito.mock(RedisConnection.class);
		RedisServerCommands serverCommands = Mockito.mock(RedisServerCommands.class);
		RedisStringCommands stringCommands = Mockito.mock(RedisStringCommands.class);
		RedisHashCommands hashCommands = Mockito.mock(RedisHashCommands.class);
		RedisSetCommands setCommands = Mockito.mock(RedisSetCommands.class);
		RedisListCommands listCommands = Mockito.mock(RedisListCommands.class);
		RedisKeyCommands keyCommands = Mockito.mock(RedisKeyCommands.class);

		Mockito.when(factory.getConnection()).thenReturn(connection);
		Mockito.when(connection.serverCommands()).thenReturn(serverCommands);
		Mockito.when(connection.stringCommands()).thenReturn(stringCommands);
		Mockito.when(connection.hashCommands()).thenReturn(hashCommands);
		Mockito.when(connection.setCommands()).thenReturn(setCommands);
		Mockito.when(connection.listCommands()).thenReturn(listCommands);
		Mockito.when(connection.keyCommands()).thenReturn(keyCommands);

		Properties serverInfo = new Properties();
		serverInfo.setProperty("redis_version", "test");
		Mockito.when(serverCommands.info()).thenReturn(serverInfo);

		// Mock specific scan commands to return empty cursors to avoid NPEs in
		// scheduled tasks
		org.springframework.data.redis.core.Cursor<byte[]> emptyCursor = Mockito
				.mock(org.springframework.data.redis.core.Cursor.class);
		Mockito.when(emptyCursor.hasNext()).thenReturn(false);
		Mockito.when(keyCommands.scan(Mockito.any())).thenReturn(emptyCursor);
		Mockito.when(connection.scan(Mockito.any())).thenReturn(emptyCursor);
		Mockito.when(setCommands.sScan(Mockito.any(), Mockito.any())).thenReturn(emptyCursor);

		return factory;
	}

	@Bean
	@Primary
	public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
		org.springframework.data.redis.core.StringRedisTemplate template = Mockito
				.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
		Mockito.when(template.execute(Mockito.any(), Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(1L);
		return template;
	}
}
