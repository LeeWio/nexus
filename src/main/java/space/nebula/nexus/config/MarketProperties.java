package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.market")
public class MarketProperties {

	private List<IndexConfig> indices;
	private ApiUrls urls;

	@Data
	public static class IndexConfig {
		private String name;
		private String symbol;
		private String hqKey;
		private MarketType type; // US or CN
	}

	@Data
	public static class ApiUrls {
		private String hq;
		private String klineCn;
		private String klineUs;
		private String klineUsDaily;
	}

	public enum MarketType {
		US, CN
	}
}
