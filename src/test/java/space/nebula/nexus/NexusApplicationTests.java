package space.nebula.nexus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import space.nebula.nexus.repository.search.PostSearchRepository;

@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class, space.nebula.nexus.config.MockRabbitMQConfig.class})
@org.springframework.test.context.ActiveProfiles("test")
class NexusApplicationTests {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Test
	void contextLoads() {
	}

}
