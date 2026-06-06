package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.FileResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface IFileService
{
	ApiResponse<FileResponse> uploadFile(MultipartFile file);

	ApiResponse<Void> deleteFile(String fileName);

	ApiResponse<PageResult<FileResponse>> searchFiles(String keyword, Pageable pageable);
}
