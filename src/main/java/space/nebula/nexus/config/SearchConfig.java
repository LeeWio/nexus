package space.nebula.nexus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.search.type", havingValue = "elasticsearch", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackages = "space.nebula.nexus.repository.search")
public class SearchConfig {
}
