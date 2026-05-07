package space.nebula.nexus.common.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local File System storage implementation.
 */
@Slf4j
@Component
public class LocalStorageProvider implements StorageProvider {

    @Value("${app.upload.location:uploads}")
    private String uploadLocation;

    @Value("${app.upload.base-url:/api/v1/public/files/}")
    private String baseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadLocation);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage directory: {}", uploadLocation, e);
            throw new RuntimeException("Storage initialization failed", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename) {
        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            // Security: Path traversal check
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new BusinessException("Cannot store file outside current directory");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            log.error("Failed to store file: {}", filename, e);
            throw new BusinessException(500, "Storage failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            throw new BusinessException(500, "Delete failed: " + e.getMessage());
        }
    }

    @Override
    public String getUrl(String filename) {
        return baseUrl + filename;
    }
}
