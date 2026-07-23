package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.payload.response.FileResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {

	FileResponse toResponse(FileMetadata metadata);

	List<FileResponse> toResponseList(List<FileMetadata> metadataList);
}
