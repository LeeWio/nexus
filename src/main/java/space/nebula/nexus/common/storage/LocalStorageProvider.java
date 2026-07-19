package space.nebula.nexus.common.storage;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Enhanced implementation for local filesystem storage.
 */
@Slf4j
public class LocalStorageProvider implements StorageProvider {

	private final Path rootLocation;
	private final String baseUrl;

	public LocalStorageProvider(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocal().getLocation()).toAbsolutePath().normalize();
		this.baseUrl = properties.getLocal().getBaseUrl();
		try {
			Files.createDirectories(this.rootLocation);
		} catch (IOException e) {
			throw new BusinessException("Storage location could not be initialized: " + this.rootLocation);
		}
	}

	@Override
	public String store(InputStream inputStream, String filename) {
		try {
			Assert.isFalse(filename.contains(".."),
					() -> new BusinessException("File path is invalid: " + filename));

			Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();

			// Security check: Ensure destination is still within rootLocation
			String destPath = destinationFile.toFile().getCanonicalPath();
			String rootPath = this.rootLocation.toFile().getCanonicalPath();
			if (!rootPath.endsWith(java.io.File.separator)) {
				rootPath += java.io.File.separator;
			}
			String finalRootPath = rootPath;
			Assert.isTrue(destPath.startsWith(finalRootPath),
					() -> new BusinessException("File path is outside the storage directory"));

			// Create parent directories if they don't exist
			Files.createDirectories(destinationFile.getParent());

			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			return filename;
		} catch (IOException e) {
			log.error("Failed to store file {}", filename, e);
			throw new BusinessException("Failed to store file: " + filename);
		}
	}

	@Override
	public void delete(String filename) {
		try {
			Path file = resolveWithinRoot(filename);
			Files.deleteIfExists(file);
		} catch (IOException e) {
			log.error("Could not delete file {}", filename, e);
		}
	}

	private Path resolveWithinRoot(String filename) throws IOException {
		Assert.isFalse(filename.contains(".."),
				() -> new BusinessException("File path is invalid"));
		Path resolved = rootLocation.resolve(filename).normalize().toAbsolutePath();
		Path canonicalRoot = rootLocation.toFile().getCanonicalFile().toPath();
		Path canonicalTarget = resolved.toFile().getCanonicalFile().toPath();
		Assert.isTrue(canonicalTarget.startsWith(canonicalRoot),
				() -> new BusinessException("File path is outside the storage directory"));
		return canonicalTarget;
	}

	@Override
	public String getUrl(String filename) {
		return baseUrl + filename;
	}
}
