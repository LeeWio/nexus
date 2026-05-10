package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.ProjectResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(ProjectRequest request);

    ProjectResponse toResponse(Project project);

    List<ProjectResponse> toResponseList(List<Project> projects);
}
