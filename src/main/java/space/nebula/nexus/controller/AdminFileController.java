package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.StorageIntegrityResponse;
import space.nebula.nexus.payload.response.StorageInventoryResponse;
import space.nebula.nexus.service.IFileService;

import java.util.concurrent.TimeUnit;

/**
 * Controller for administrative file operations. Handles secure file uploads
 * and management of stored assets.
 */
@Tag(name = "Admin File Management", description = "Endpoints for asset management and file uploads")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

	private final IFileService fileService;

	@GetMapping
	@Operation(summary = "Search files", description = "Retrieve a paginated list of all uploaded files with optional keyword search.")
	public ApiResponse<PageResult<FileResponse>> searchFiles(
			@Parameter(description = "Keyword to search in original or unique filename") @RequestParam(required = false) String keyword,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
		return fileService.searchFiles(keyword, pageable);
	}

	@GetMapping("/inventory")
	@Operation(summary = "Get storage inventory", description = "Returns aggregate asset metadata for storage and backup verification.")
	public ApiResponse<StorageInventoryResponse> getStorageInventory() {
		return fileService.getStorageInventory();
	}

	@GetMapping("/integrity")
	@Operation(summary = "Verify storage integrity", description = "Checks one stable page of active assets against the configured storage provider.")
	public ApiResponse<StorageIntegrityResponse> verifyStorageIntegrity(
			@Parameter(description = "Pagination parameters. At most 200 assets are checked per request.") @PageableDefault(size = 100) Pageable pageable) {
		return fileService.verifyStorageIntegrity(pageable);
	}

	@PostMapping("/upload")
	@Operation(summary = "Upload a file", description = "Securely uploads a file and returns its metadata including URLs.")
	@RateLimit(count = 20, time = 1, unit = TimeUnit.MINUTES, message = "Too many file uploads. Please wait a moment.")
	public ApiResponse<FileResponse> uploadFile(
			@Parameter(description = "The file payload to upload") @RequestParam("file") MultipartFile file) {
		return fileService.uploadFile(file);
	}

	@DeleteMapping("/{fileName}")
	@Operation(summary = "Delete a file", description = "Permanently removes a file and its associated metadata from storage.")
	public ApiResponse<Void> deleteFile(
			@Parameter(description = "The unique stored name of the file") @PathVariable String fileName) {
		return fileService.deleteFile(fileName);
	}
}
