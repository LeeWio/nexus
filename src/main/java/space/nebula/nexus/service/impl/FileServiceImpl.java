package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.mapper.FileMapper;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IFileService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Professional implementation of File Management Service.
 * Ensures synchronization between physical storage and metadata repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".txt");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "text/plain");

    private final StorageProvider storageProvider;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public ApiResponse<FileResponse> uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Cannot process an empty file upload request");
        }

        validateFileIntegrity(file);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = extractFileExtension(originalFilename);
        String uniqueStoredName = UUID.randomUUID().toString().replace("-", "") + extension;

        // Try to get current user as uploader
        User uploader = null;
        try {
            uploader = SecurityUtil.getCurrentUserOrThrow(userRepository);
        } catch (Exception e) {
            log.debug("Unauthenticated file upload: {}", originalFilename);
        }

        try {
            // 1. Persist to physical/cloud storage
            storageProvider.store(file.getInputStream(), uniqueStoredName);
            log.debug("Physical file stored successfully: {}", uniqueStoredName);

            // 2. Persist metadata to database
            FileMetadata metadata = new FileMetadata();
            metadata.setFileName(uniqueStoredName);
            metadata.setOriginalName(originalFilename);
            metadata.setFileUrl(storageProvider.getUrl(uniqueStoredName));
            metadata.setFileSize(file.getSize());
            metadata.setFileType(file.getContentType());
            metadata.setUploader(uploader);
            
            FileMetadata savedMetadata = fileRepository.save(metadata);
            log.info("File metadata indexed in database. ID: {}, Name: {}", savedMetadata.getId(), uniqueStoredName);

            return ApiResponse.success("File uploaded and indexed successfully", fileMapper.toResponse(savedMetadata));

        } catch (IOException e) {
            log.error("Critical I/O error during file upload: {}", originalFilename, e);
            throw new BusinessException(500, "Internal server error during file persistence");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteFile(String fileName) {
        String sanitizedFileName = StringUtils.cleanPath(fileName);
        if (sanitizedFileName.contains("..")) {
            throw new BusinessException(400, "Security violation: Invalid filename path");
        }

        // 1. Locate metadata first
        FileMetadata metadata = fileRepository.findByFileName(sanitizedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("File", "name", sanitizedFileName));

        // 2. Remove physical file
        try {
            storageProvider.delete(sanitizedFileName);
            log.debug("Physical file removed: {}", sanitizedFileName);
        } catch (Exception e) {
            log.error("Failed to remove physical file: {}. Purge aborted.", sanitizedFileName, e);
            throw new BusinessException(500, "Physical file removal failed. Database record preserved.");
        }

        // 3. Remove metadata
        fileRepository.delete(metadata);
        log.info("File and metadata purged for: {}", sanitizedFileName);

        return ApiResponse.success("File deleted successfully", null);
    }

    private void validateFileIntegrity(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Security rejection: Forbidden MIME type: {}", contentType);
            throw new BusinessException(400, "File content type not supported");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = extractFileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("Security rejection: Forbidden extension: {}", extension);
                throw new BusinessException(400, "File extension not allowed");
            }
        }
    }

    private String extractFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex) : "";
    }
}
