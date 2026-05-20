package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.service.IFileService;

/**
 * Controller for administrative file operations.
 * Handles secure file uploads and management of stored assets.
 */
@Tag(name = "Admin File Management", description = "Endpoints for asset management and file uploads")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

    private final IFileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file", description = "Securely uploads a file and returns its metadata including URLs.")
    public ApiResponse<FileResponse> uploadFile(
            @Parameter(description = "The file payload to upload") 
            @RequestParam("file") MultipartFile file) {
        return fileService.uploadFile(file);
    }

    @DeleteMapping("/{fileName}")
    @Operation(summary = "Delete a file", description = "Permanently removes a file and its associated metadata from storage.")
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "The unique stored name of the file") 
            @PathVariable String fileName) {
        return fileService.deleteFile(fileName);
    }
}
