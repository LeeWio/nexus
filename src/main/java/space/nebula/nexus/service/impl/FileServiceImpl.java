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
 * Enhanced with deep MIME detection, security validation, and automated image processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "text/plain"
    );

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
            throw new BusinessException(BusinessCode.BAD_REQUEST, "Cannot process empty file payload");
        }

        try {
            var fileBytes = file.getBytes();
            var originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            var extension = extractFileExtension(originalFilename);
            
            // 1. Deep MIME detection for security
            String detectedMimeType;
            try (var bais = new ByteArrayInputStream(fileBytes)) {
                detectedMimeType = fileUtil.detectMimeType(bais);
            }
            log.debug("Deep MIME verification: detected {} for file {}", detectedMimeType, originalFilename);

            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                log.warn("Security rejection: Unsupported MIME type {}", detectedMimeType);
                throw new BusinessException(BusinessCode.BAD_REQUEST, "File content type not supported");
            }

            var uniqueName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 2. Specialized Image Processing
            String thumbnailUrl = null;
            Integer width = null, height = null;

            if (fileUtil.isImage(detectedMimeType)) {
                var dimensions = fileUtil.getImageDimensions(fileBytes);
                if (dimensions != null) {
                    width = dimensions.width();
                    height = dimensions.height();
                }

                try {
                    var thumbnailBytes = fileUtil.generateThumbnail(fileBytes, 200, 200);
                    var thumbnailName = "thumb_" + uniqueName.substring(0, uniqueName.lastIndexOf('.')) + ".jpg";
                    storageProvider.store(new ByteArrayInputStream(thumbnailBytes), thumbnailName);
                    thumbnailUrl = storageProvider.getUrl(thumbnailName);
                } catch (Exception e) {
                    log.warn("Non-critical failure in thumbnail generation: {}", e.getMessage());
                }
            }

            // 3. Asset Persistence
            storageProvider.store(new ByteArrayInputStream(fileBytes), uniqueName);

            User uploader = null;
            try {
                uploader = SecurityUtil.getCurrentUserOrThrow(userRepository);
            } catch (Exception e) {
                log.debug("File uploaded by anonymous/system process");
            }

            var metadata = new FileMetadata();
            metadata.setFileName(uniqueName);
            metadata.setOriginalName(originalFilename);
            metadata.setFileUrl(storageProvider.getUrl(uniqueName));
            metadata.setFileSize((long) fileBytes.length);
            metadata.setFileType(detectedMimeType);
            metadata.setThumbnailUrl(thumbnailUrl);
            metadata.setWidth(width);
            metadata.setHeight(height);
            metadata.setUploader(uploader);
            
            var savedFile = fileRepository.save(metadata);
            log.info("File asset indexed successfully: {} (ID: {})", uniqueName, savedFile.getId());

            return ApiResponse.success("File uploaded successfully", fileMapper.toResponse(savedFile));

        } catch (IOException e) {
            log.error("Fatal I/O error during file processing: {}", file.getOriginalFilename(), e);
            throw new BusinessException(BusinessCode.ERROR, "System failed to process the file");
        }
    }

    @Override
    @Transactional
    @LogOperation("Delete File")
    public ApiResponse<Void> deleteFile(String fileName) {
        var sanitizedName = StringUtils.cleanPath(fileName);
        if (sanitizedName.contains("..")) {
            throw new BusinessException(BusinessCode.BAD_REQUEST, "Security violation: Invalid path");
        }

        var metadata = fileRepository.findByFileName(sanitizedName)
                .orElseThrow(() -> new ResourceNotFoundException("File", "name", sanitizedName));

        storageProvider.delete(sanitizedName);

        if (metadata.getThumbnailUrl() != null) {
            var thumbnailName = extractFileNameFromUrl(metadata.getThumbnailUrl());
            storageProvider.delete(thumbnailName);
        }

        fileRepository.delete(metadata);
        log.info("File asset and related records purged for: {}", sanitizedName);

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
