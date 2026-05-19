package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
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
import space.nebula.nexus.utils.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Professional implementation of File Management Service.
 * Enhanced with deep MIME detection and image processing (thumbnails).
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
    private final FileUtil fileUtil;

    @Override
    @Transactional
    @LogOperation("Upload File")
    public ApiResponse<FileResponse> uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BAD_REQUEST, "Cannot process an empty file upload request");
        }

        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = extractFileExtension(originalFilename);
            
            // 1. Deep MIME detection
            String detectedMimeType;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
                detectedMimeType = fileUtil.detectMimeType(bais);
            }
            log.debug("Detected MIME type: {} for file: {}", detectedMimeType, originalFilename);

            // 2. Validate against allowed list
            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                log.warn("Security rejection: Deep MIME detection failed for type: {}", detectedMimeType);
                throw new BusinessException(BusinessCode.BAD_REQUEST, "File content type not supported or spoofed");
            }

            String uniqueStoredName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 3. Image specific processing
            String thumbnailUrl = null;
            Integer width = null;
            Integer height = null;

            if (fileUtil.isImage(detectedMimeType)) {
                // Get dimensions
                FileUtil.ImageDimensions dimensions = fileUtil.getImageDimensions(fileBytes);
                if (dimensions != null) {
                    width = dimensions.width();
                    height = dimensions.height();
                }

                // Generate and store thumbnail
                try {
                    byte[] thumbnailBytes = fileUtil.generateThumbnail(fileBytes, 200, 200);
                    String thumbnailName = "thumb_" + uniqueStoredName.substring(0, uniqueStoredName.lastIndexOf('.')) + ".jpg";
                    storageProvider.store(new ByteArrayInputStream(thumbnailBytes), thumbnailName);
                    thumbnailUrl = storageProvider.getUrl(thumbnailName);
                    log.debug("Thumbnail created: {}", thumbnailName);
                } catch (Exception e) {
                    log.warn("Thumbnail generation failed for {}: {}", originalFilename, e.getMessage());
                }
            }

            // 4. Store original file
            storageProvider.store(new ByteArrayInputStream(fileBytes), uniqueStoredName);

            // 5. Persist metadata
            User uploader = null;
            try {
                uploader = SecurityUtil.getCurrentUserOrThrow(userRepository);
            } catch (Exception e) {
                log.debug("Unauthenticated file upload: {}", originalFilename);
            }

            FileMetadata metadata = new FileMetadata();
            metadata.setFileName(uniqueStoredName);
            metadata.setOriginalName(originalFilename);
            metadata.setFileUrl(storageProvider.getUrl(uniqueStoredName));
            metadata.setFileSize((long) fileBytes.length);
            metadata.setFileType(detectedMimeType);
            metadata.setThumbnailUrl(thumbnailUrl);
            metadata.setWidth(width);
            metadata.setHeight(height);
            metadata.setUploader(uploader);
            
            FileMetadata savedMetadata = fileRepository.save(metadata);
            log.info("File processed and indexed. ID: {}, Name: {}", savedMetadata.getId(), uniqueStoredName);

            return ApiResponse.success("File uploaded and processed successfully", fileMapper.toResponse(savedMetadata));

        } catch (IOException e) {
            log.error("Critical error during file processing: {}", file.getOriginalFilename(), e);
            throw new BusinessException(BusinessCode.ERROR, "Internal error during file processing");
        }
    }

    @Override
    @Transactional
    @LogOperation("Delete File")
    public ApiResponse<Void> deleteFile(String fileName) {
        String sanitizedFileName = StringUtils.cleanPath(fileName);
        if (sanitizedFileName.contains("..")) {
            throw new BusinessException(BusinessCode.BAD_REQUEST, "Security violation: Invalid filename path");
        }

        FileMetadata metadata = fileRepository.findByFileName(sanitizedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("File", "name", sanitizedFileName));

        // Remove original
        storageProvider.delete(sanitizedFileName);

        // Remove thumbnail if exists
        if (metadata.getThumbnailUrl() != null) {
            String thumbnailName = extractFileNameFromUrl(metadata.getThumbnailUrl());
            storageProvider.delete(thumbnailName);
        }

        fileRepository.delete(metadata);
        log.info("File and related resources purged for: {}", sanitizedFileName);

        return ApiResponse.success("File deleted successfully", null);
    }

    private String extractFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex) : "";
    }

    private String extractFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
