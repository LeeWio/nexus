package space.nebula.nexus.common.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.StorageProperties;

import java.io.InputStream;

/**
 * Implementation of StorageProvider using Aliyun OSS.
 */
@Slf4j
public class AliyunStorageProvider implements StorageProvider {

	private final StorageProperties.AliyunConfig config;

	public AliyunStorageProvider(StorageProperties.AliyunConfig config) {
		this.config = config;
	}

	@Override
	public String store(InputStream inputStream, String originalFilename) {
		String extension = originalFilename != null && originalFilename.contains(".")
				? originalFilename.substring(originalFilename.lastIndexOf("."))
				: "";
		String fileName = IdUtil.fastSimpleUUID() + extension;

		OSS ossClient = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(),
				config.getAccessKeySecret());

		try {
			ObjectMetadata metadata = new ObjectMetadata();
			// We don't have MultipartFile here, so we might need to guess content type or
			// set generic
			metadata.setContentType("application/octet-stream");

			PutObjectRequest putObjectRequest = new PutObjectRequest(config.getBucketName(), fileName, inputStream,
					metadata);
			ossClient.putObject(putObjectRequest);

			log.info("Successfully uploaded file to Aliyun OSS: {}", fileName);
			return fileName; // Return the key/filename
		} catch (Exception e) {
			log.error("Failed to upload file to Aliyun OSS", e);
			throw new BusinessException(500, "Cloud storage upload failed");
		} finally {
			ossClient.shutdown();
		}
	}

	@Override
	public void delete(String fileName) {
		OSS ossClient = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(),
				config.getAccessKeySecret());
		try {
			ossClient.deleteObject(config.getBucketName(), fileName);
			log.info("Successfully deleted file from Aliyun OSS: {}", fileName);
		} catch (Exception e) {
			log.error("Failed to delete file from Aliyun OSS: {}", fileName, e);
		} finally {
			ossClient.shutdown();
		}
	}

	@Override
	public String getUrl(String fileName) {
		if (fileName == null || fileName.isBlank())
			return null;
		if (fileName.startsWith("http"))
			return fileName;
		return config.getDomain() + "/" + fileName;
	}

	@Override
	public String getSignedUrl(String fileName, long expireSeconds) {
		if (fileName == null || fileName.isBlank())
			return null;

		OSS ossClient = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(),
				config.getAccessKeySecret());
		try {
			java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expireSeconds * 1000);
			return ossClient.generatePresignedUrl(config.getBucketName(), fileName, expiration).toString();
		} catch (Exception e) {
			log.error("Failed to generate signed URL for Aliyun OSS", e);
			return getUrl(fileName); // Fallback to public
		} finally {
			ossClient.shutdown();
		}
	}
}
