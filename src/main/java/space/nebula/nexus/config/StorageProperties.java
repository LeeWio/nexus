package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties
{

	/**
	 * Storage type: local, aliyun, s3
	 */
	private String type = "local";

	/**
	 * List of allowed MIME types for upload.
	 */
	private java.util.List<String> allowedMimeTypes = java.util.Arrays.asList("image/jpeg", "image/png", "image/gif",
			"image/webp", "application/pdf", "text/plain");

	/**
	 * Maximum allowed file size in bytes. Default 10MB.
	 */
	private long maxFileSize = 10485760;

	private LocalConfig local = new LocalConfig();
	private AliyunConfig aliyun = new AliyunConfig();
	private S3Config s3 = new S3Config();

	@Data
	public static class LocalConfig
	{
		private String location = "uploads";
		private String baseUrl = "/api/v1/public/files/";
	}

	@Data
	public static class AliyunConfig
	{
		private String endpoint;
		private String accessKeyId;
		private String accessKeySecret;
		private String bucketName;
		private String domain;
	}

	@Data
	public static class S3Config
	{
		private String endpoint;
		private String accessKeyId;
		private String accessKeySecret;
		private String bucketName;
		private String region;
		private String domain;
	}
}
