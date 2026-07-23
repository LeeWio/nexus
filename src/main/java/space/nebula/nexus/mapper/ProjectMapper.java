package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.ProjectResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ProjectMapper {

	ProjectResponse toResponse(Project project);

	List<ProjectResponse> toResponseList(List<Project> projects);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "starsCount", ignore = true)
	@Mapping(target = "forksCount", ignore = true)
	@Mapping(target = "language", ignore = true)
	@Mapping(target = "repoName", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	Project toEntity(ProjectRequest request);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "starsCount", ignore = true)
	@Mapping(target = "forksCount", ignore = true)
	@Mapping(target = "language", ignore = true)
	@Mapping(target = "repoName", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget Project project, ProjectRequest request);
}
