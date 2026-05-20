package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

	/**
	 * Storage type: local, aliyun, s3
	 */
	private String type = "local";

	private LocalConfig local = new LocalConfig();
	private AliyunConfig aliyun = new AliyunConfig();

	@Data
	public static class LocalConfig {
		private String location = "uploads";
		private String baseUrl = "/api/v1/public/files/";
	}

	@Data
	public static class AliyunConfig {
		private String endpoint;
		private String accessKeyId;
		private String accessKeySecret;
		private String bucketName;
		private String domain;
	}
}
