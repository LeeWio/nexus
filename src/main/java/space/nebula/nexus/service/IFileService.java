package space.nebula.nexus.service;

import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.FileResponse;

public interface IFileService
{
	ApiResponse<FileResponse> uploadFile(MultipartFile file);

	ApiResponse<Void> deleteFile(String fileName);
}
