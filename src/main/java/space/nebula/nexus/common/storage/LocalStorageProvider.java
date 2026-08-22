package space.nebula.nexus.common.storage;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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
			Path destinationFile = validateFilenameAndResolve(filename);

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
			Path file = validateFilenameAndResolve(filename);
			Files.deleteIfExists(file);
		} catch (IOException e) {
			log.error("Could not delete file {}", filename, e);
		}
	}

	@Override
	public boolean exists(String filename) {
		try {
			BasicFileAttributes attributes = Files.readAttributes(validateFilenameAndResolve(filename),
					BasicFileAttributes.class);
			return attributes.isRegularFile();
		} catch (NoSuchFileException e) {
			return false;
		} catch (IOException e) {
			log.error("Could not inspect local storage object {}", filename, e);
			throw new BusinessException("Could not inspect local storage object");
		}
	}

	private Path validateFilenameAndResolve(String filename) throws IOException {
		// Strict check: No parent directory references allowed to prevent traversal
		// attacks,
		// but allow subfolders/separators for structured paths (e.g.,
		// static/posts/file.html)
		Assert.isFalse(filename.contains(".."), () -> new BusinessException("Invalid filename format"));

		Path resolved = rootLocation.resolve(filename).normalize().toAbsolutePath();

		// Additional safety check: Ensure destination is still within rootLocation
		String destPath = resolved.toFile().getCanonicalPath();
		String rootPath = this.rootLocation.toFile().getCanonicalPath();
		if (!rootPath.endsWith(java.io.File.separator)) {
			rootPath += java.io.File.separator;
		}

		Assert.isTrue(destPath.startsWith(rootPath),
				() -> new BusinessException("File path is outside the storage directory"));

		return resolved;
	}

	@Override
	public String getUrl(String filename) {
		return baseUrl + filename;
	}
}
