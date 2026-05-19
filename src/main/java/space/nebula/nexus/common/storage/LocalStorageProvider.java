package space.nebula.nexus.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.StorageProperties;

import java.io.File;
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
            throw new BusinessException("Could not initialize storage location: " + this.rootLocation);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename) {
        try {
            if (filename.contains("..")) {
                throw new BusinessException("Cannot store file with relative path outside current directory " + filename);
            }
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            // Security check
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new BusinessException("Cannot store file outside specified directory");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            log.error("Failed to store file {}", filename, e);
            throw new BusinessException("Failed to store file " + filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Could not delete file {}", filename, e);
        }
    }

    @Override
    public String getUrl(String filename) {
        return baseUrl + filename;
    }
}
