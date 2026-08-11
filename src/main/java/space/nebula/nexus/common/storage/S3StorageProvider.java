package space.nebula.nexus.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@RequiredArgsConstructor
public class S3StorageProvider implements StorageProvider {

	private final StorageProperties.S3Config config;

	private S3Client getClient() {
		return S3Client.builder().region(Region.of(config.getRegion()))
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(config.getAccessKeyId(), config.getAccessKeySecret())))
				.endpointOverride(URI.create(config.getEndpoint())).build();
	}

	@Override
	public String store(InputStream inputStream, String filename) {
		try (S3Client client = getClient()) {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(config.getBucketName()).key(filename)
					.build();

			client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
			return filename;
		} catch (IOException e) {
			log.error("Failed to store file to S3: {}", filename, e);
			throw new RuntimeException("Storage error", e);
		}
	}

	@Override
	public void delete(String filename) {
		try (S3Client client = getClient()) {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(config.getBucketName())
					.key(filename).build();
			client.deleteObject(deleteObjectRequest);
		} catch (Exception e) {
			log.error("Failed to delete file from S3: {}", filename, e);
		}
	}

	@Override
	public boolean exists(String filename) {
		try (S3Client client = getClient()) {
			HeadObjectRequest request = HeadObjectRequest.builder().bucket(config.getBucketName()).key(filename)
					.build();
			client.headObject(request);
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return false;
			}
			log.error("Failed to inspect S3 object {}", filename, e);
			throw new BusinessException(500, "Could not inspect S3 storage object");
		} catch (Exception e) {
			log.error("Failed to inspect S3 object {}", filename, e);
			throw new BusinessException(500, "Could not inspect S3 storage object");
		}
	}

	@Override
	public String getUrl(String filename) {
		if (config.getDomain() != null && !config.getDomain().isBlank()) {
			return config.getDomain() + "/" + filename;
		}
		return config.getEndpoint() + "/" + config.getBucketName() + "/" + filename;
	}
}
