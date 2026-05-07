package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.service.IFileService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Enhanced FileService with storage abstraction and security validation.
 */
@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".txt");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "text/plain");

    @Resource
    private StorageProvider storageProvider;

    @Override
    public ApiResponse<FileResponse> uploadFile(MultipartFile file) {
        // 1. Basic validation
        if (file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file");
        }

        // 2. Security validation: MIME type and extension
        validateFile(file);

        // 3. Generate sanitized random filename
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getExtension(originalFilename);
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            // 4. Delegate to storage provider
            storageProvider.store(file.getInputStream(), newFileName);
            
            log.info("File uploaded successfully: {} (Type: {})", newFileName, file.getContentType());
            
            FileResponse response = new FileResponse(
                    newFileName, 
                    storageProvider.getUrl(newFileName), 
                    file.getSize(), 
                    file.getContentType()
            );
            return ApiResponse.success("File uploaded successfully", response);

        } catch (IOException e) {
            log.error("Failed to read file input stream", e);
            throw new BusinessException(500, "File upload failed: internal error");
        }
    }

    @Override
    public ApiResponse<Void> deleteFile(String fileName) {
        // Security: Prevent path traversal by cleaning the input
        String cleanName = StringUtils.cleanPath(fileName);
        if (cleanName.contains("..")) {
            throw new BusinessException("Invalid filename");
        }
        
        storageProvider.delete(cleanName);
        log.info("File deleted: {}", cleanName);
        return ApiResponse.success("File deleted successfully", null);
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Blocked file upload with forbidden MIME type: {}", contentType);
            throw new BusinessException("File type not allowed: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("Blocked file upload with forbidden extension: {}", extension);
                throw new BusinessException("File extension not allowed: " + extension);
            }
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i) : "";
    }
}
