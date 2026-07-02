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
import space.nebula.nexus.payload.response.PageResult;
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

	private final StorageProvider storageProvider;
	private final FileRepository fileRepository;
	private final UserRepository userRepository;
	private final FileMapper fileMapper;
	private final FileUtil fileUtil;
	private final space.nebula.nexus.config.StorageProperties storageProperties;

	@Override
	@Transactional
	@LogOperation("Upload File")
	public ApiResponse<FileResponse> uploadFile(MultipartFile file)
	{
		Assert.isFalse(file.isEmpty(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Cannot process empty file payload"));

		if (file.getSize() > storageProperties.getMaxFileSize())
		{
			throw new BusinessException(BusinessCode.FILE_SIZE_LIMIT,
					formatFileSize(file.getSize()),
					formatFileSize(storageProperties.getMaxFileSize()));
		}

		try
		{
			// 1. Content-based Deduplication (SHA-256) - Streaming approach
			String fileHash;
			try (var is = file.getInputStream())
			{
				fileHash = SecureUtil.sha256(is);
			}

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

			// 2. Deep MIME detection for security - Streaming approach
			String detectedMimeType;
			try (var is = file.getInputStream())
			{
				detectedMimeType = fileUtil.detectMimeType(is);
			}
			log.debug("Deep MIME verification: detected {} for file {}", detectedMimeType, originalFilename);

			// SVG XML-XSS script injection defense
			if ("image/svg+xml".equals(detectedMimeType) || ".svg".equalsIgnoreCase(extension))
			{
				checkSvgScriptSecurity(file.getBytes());
			}

			if (!storageProperties.getAllowedMimeTypes().contains(detectedMimeType))
			{
				log.warn("Security rejection: Unsupported MIME type {}", detectedMimeType);
				throw new BusinessException(BusinessCode.FILE_TYPE_NOT_SUPPORTED,
						detectedMimeType,
						String.join(", ", storageProperties.getAllowedMimeTypes()));
			}

			var uniqueName = IdUtil.fastSimpleUUID() + extension;
			String thumbnailUrl = null;
			Integer width = null, height = null;
			long finalSize = file.getSize();

			// 3. specialized processing for images
			if (fileUtil.isImage(detectedMimeType))
			{
				byte[] fileBytes = file.getBytes();

				// Strip private EXIF metadata from JPEG and PNG images
				if ("image/jpeg".equals(detectedMimeType))
				{
					fileBytes = stripImageMetadata(fileBytes, "jpeg");
				}
				else if ("image/png".equals(detectedMimeType))
				{
					fileBytes = stripImageMetadata(fileBytes, "png");
				}

				// Automatically convert to WebP for better performance
				if (!"image/webp".equals(detectedMimeType)) {
					try {
						byte[] webpBytes = fileUtil.convertToWebP(fileBytes);
						if (webpBytes.length < fileBytes.length) {
							log.info("Converted image to WebP, size reduced from {} to {} bytes", fileBytes.length, webpBytes.length);
							fileBytes = webpBytes;
							detectedMimeType = "image/webp";
							uniqueName = IdUtil.fastSimpleUUID() + ".webp";
							
							// Re-calculate hash for deduplication based on converted content
							fileHash = SecureUtil.sha256(new ByteArrayInputStream(fileBytes));
							var existingWebp = fileRepository.findByFileHash(fileHash);
							if (existingWebp.isPresent()) {
								FileMetadata metadata = existingWebp.get();
								metadata.setReferenceCount(metadata.getReferenceCount() + 1);
								fileRepository.save(metadata);
								return ApiResponse.success("WebP version reused via deduplication", fileMapper.toResponse(metadata));
							}
						}
					} catch (Throwable t) {
						log.warn("WebP conversion failed, falling back to original format: {}", t.getMessage());
					}
				}

				var dimensions = fileUtil.getImageDimensions(fileBytes);
				if (ObjectUtil.isNotNull(dimensions))
				{
					width = dimensions.width();
					height = dimensions.height();
				}

				try
				{
					var thumbnailBytes = fileUtil.convertToWebP(fileUtil.generateThumbnail(fileBytes, 300, 300));
					var thumbnailName = "thumb_" + uniqueName.substring(0, uniqueName.lastIndexOf('.')) + ".webp";
					storageProvider.store(new ByteArrayInputStream(thumbnailBytes), thumbnailName);
					thumbnailUrl = storageProvider.getUrl(thumbnailName);
					log.info("Generated WebP thumbnail: {}", thumbnailName);
				}
				catch (Throwable t)
				{
					log.warn("Non-critical failure in thumbnail generation: {}", t.getMessage());
				}
				
				// Final storage with potentially converted bytes
				storageProvider.store(new ByteArrayInputStream(fileBytes), uniqueName);
				finalSize = (long) fileBytes.length;
			}
			else 
			{
				// 4. Non-image Asset Persistence
				try (var is = file.getInputStream())
				{
					storageProvider.store(is, uniqueName);
				}
				finalSize = file.getSize();
			}

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
			metadata.setFileSize(finalSize);
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
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<FileResponse>> searchFiles(String keyword, org.springframework.data.domain.Pageable pageable)
	{
		org.springframework.data.domain.Page<FileMetadata> page;
		if (StrUtil.isNotBlank(keyword))
		{
			page = fileRepository.findByOriginalNameContainingIgnoreCaseOrFileNameContainingIgnoreCase(keyword, keyword,
					pageable);
		}
		else
		{
			page = fileRepository.findAll(pageable);
		}
		return ApiResponse.success(PageResult.of(page.map(fileMapper::toResponse)));
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

	private String formatFileSize(long size)
	{
		if (size <= 0) return "0 B";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	private void checkSvgScriptSecurity(byte[] fileBytes)
	{
		try
		{
			String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
			String lowerContent = content.toLowerCase();

			// Scan for malicious tags, javascript schemes, or XML External Entity (XXE) vectors
			if (lowerContent.contains("<script") 
					|| lowerContent.contains("javascript:") 
					|| lowerContent.contains("onload=")
					|| lowerContent.contains("onerror=")
					|| lowerContent.contains("<!entity") 
					|| lowerContent.contains("<!doctype"))
			{
				log.warn("Security violation: Malicious scripts or XML entities detected in uploaded SVG file.");
				throw new BusinessException(BusinessCode.BAD_REQUEST, 
						"SVG file is rejected due to security risk (potential XSS/XXE scripts detected).");
			}
		}
		catch (BusinessException ex)
		{
			throw ex;
		}
		catch (Exception e)
		{
			log.warn("Failed to parse SVG file for security scan: {}", e.getMessage());
		}
	}

	private byte[] stripImageMetadata(byte[] fileBytes, String format)
	{
		try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fileBytes);
			 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream())
		{
			java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bais);
			if (image != null)
			{
				// Reading and writing via ImageIO naturally drops all EXIF/app segments!
				boolean success = javax.imageio.ImageIO.write(image, format, baos);
				if (success)
				{
					log.info("Successfully stripped EXIF/metadata from uploaded {} image.", format);
					return baos.toByteArray();
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to strip image metadata, falling back to original: {}", e.getMessage());
		}
		return fileBytes;
	}
}
