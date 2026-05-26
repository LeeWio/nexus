package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
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

/**
 * Professional implementation of File Management Service. Enhanced with deep
 * MIME detection, security validation, automated image processing, and
 * content-based deduplication using SHA-256.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService
{

	private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif",
			"image/webp", "application/pdf", "text/plain");

	private final StorageProvider storageProvider;
	private final FileRepository fileRepository;
	private final UserRepository userRepository;
	private final FileMapper fileMapper;
	private final FileUtil fileUtil;

	@Override
	@Transactional
	@LogOperation("Upload File")
	public ApiResponse<FileResponse> uploadFile(MultipartFile file)
	{
		if (file.isEmpty())
		{
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Cannot process empty file payload");
		}

		try
		{
			var fileBytes = file.getBytes();

			// 1. Content-based Deduplication (SHA-256)
			String fileHash = SecureUtil.sha256(new ByteArrayInputStream(fileBytes));
			var existingFile = fileRepository.findByFileHash(fileHash);
			if (existingFile.isPresent())
			{
				FileMetadata metadata = existingFile.get();
				metadata.setReferenceCount(metadata.getReferenceCount() + 1);
				fileRepository.save(metadata);
				log.info("File deduplicated: existing asset reused (ID: {})", metadata.getId());
				return ApiResponse.success("File reused via deduplication", fileMapper.toResponse(metadata));
			}

			var originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
			var extension = extractFileExtension(originalFilename);

			// 2. Deep MIME detection for security
			String detectedMimeType;
			try (var bais = new ByteArrayInputStream(fileBytes))
			{
				detectedMimeType = fileUtil.detectMimeType(bais);
			}
			log.debug("Deep MIME verification: detected {} for file {}", detectedMimeType, originalFilename);

			if (!ALLOWED_MIME_TYPES.contains(detectedMimeType))
			{
				log.warn("Security rejection: Unsupported MIME type {}", detectedMimeType);
				throw new BusinessException(BusinessCode.BAD_REQUEST, "File content type not supported");
			}

			var uniqueName = IdUtil.fastSimpleUUID() + extension;

			// 3. Specialized Image Processing
			String thumbnailUrl = null;
			Integer width = null, height = null;

			if (fileUtil.isImage(detectedMimeType))
			{
				var dimensions = fileUtil.getImageDimensions(fileBytes);
				if (ObjectUtil.isNotNull(dimensions))
				{
					width = dimensions.width();
					height = dimensions.height();
				}

				try
				{
					var thumbnailBytes = fileUtil.generateThumbnail(fileBytes, 200, 200);
					var thumbnailName = "thumb_" + uniqueName.substring(0, uniqueName.lastIndexOf('.')) + ".jpg";
					storageProvider.store(new ByteArrayInputStream(thumbnailBytes), thumbnailName);
					thumbnailUrl = storageProvider.getUrl(thumbnailName);
				}
				catch (Exception e)
				{
					log.warn("Non-critical failure in thumbnail generation: {}", e.getMessage());
				}
			}

			// 4. Asset Persistence
			storageProvider.store(new ByteArrayInputStream(fileBytes), uniqueName);

			User uploader = null;
			try
			{
				uploader = SecurityUtil.getCurrentUserOrThrow(userRepository);
			}
			catch (Exception e)
			{
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
			metadata.setFileHash(fileHash);
			metadata.setReferenceCount(1);

			var savedFile = fileRepository.save(metadata);
			log.info("File asset indexed successfully: {} (ID: {})", uniqueName, savedFile.getId());

			return ApiResponse.success("File uploaded successfully", fileMapper.toResponse(savedFile));

		}
		catch (IOException e)
		{
			log.error("Fatal I/O error during file processing: {}", file.getOriginalFilename(), e);
			throw new BusinessException(BusinessCode.ERROR, "System failed to process the file");
		}
	}

	@Override
	@Transactional
	@LogOperation("Delete File")
	public ApiResponse<Void> deleteFile(String fileName)
	{
		var sanitizedName = StringUtils.cleanPath(fileName);
		Assert.isFalse(sanitizedName.contains(".."),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Security violation: Invalid path"));

		var metadata = fileRepository.findByFileName(sanitizedName)
				.orElseThrow(() -> new ResourceNotFoundException("File", "name", sanitizedName));

		// 1. Decrement reference count
		metadata.setReferenceCount(metadata.getReferenceCount() - 1);

		if (metadata.getReferenceCount() > 0)
		{
			fileRepository.save(metadata);
			log.info("File reference decremented for: {}. Current count: {}", sanitizedName,
					metadata.getReferenceCount());
			return ApiResponse.success("File reference removed", null);
		}

		// 2. If count reaches zero, purge physical files and DB record
		storageProvider.delete(sanitizedName);

		if (StrUtil.isNotBlank(metadata.getThumbnailUrl()))
		{
			var thumbnailName = extractFileNameFromUrl(metadata.getThumbnailUrl());
			storageProvider.delete(thumbnailName);
		}

		fileRepository.delete(metadata);
		log.info("File asset and related records purged for: {}", sanitizedName);

		return ApiResponse.success("File permanently deleted", null);
	}

	private String extractFileExtension(String filename)
	{
		int dotIndex = filename.lastIndexOf('.');
		return (dotIndex > 0) ? filename.substring(dotIndex) : "";
	}

	private String extractFileNameFromUrl(String url)
	{
		return url.substring(url.lastIndexOf('/') + 1);
	}
}
