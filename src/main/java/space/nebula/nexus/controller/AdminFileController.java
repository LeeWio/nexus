package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.service.IFileService;

@RestController
@RequestMapping("/api/v1/admin/files")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin File Management", description = "Endpoints for uploading and deleting files (Requires ADMIN role)")
public class AdminFileController {

    @Resource
    private IFileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    public ApiResponse<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        return fileService.uploadFile(file);
    }

    @DeleteMapping("/{fileName}")
    @Operation(summary = "Delete a file")
    public ApiResponse<Void> deleteFile(@PathVariable String fileName) {
        return fileService.deleteFile(fileName);
    }
}
