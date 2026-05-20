package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.ProjectResponse;

import java.util.List;

public interface IProjectService {

	// Admin methods
	ApiResponse<PageResult<ProjectResponse>> getAdminProjects(Pageable pageable);
	ApiResponse<ProjectResponse> getProjectById(Long id);
	ApiResponse<ProjectResponse> createProject(ProjectRequest request);
	ApiResponse<ProjectResponse> updateProject(Long id, ProjectRequest request);
	ApiResponse<Void> deleteProject(Long id);

	// Public methods
	ApiResponse<List<ProjectResponse>> getPublicProjects();
}
