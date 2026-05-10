package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.mapper.ProjectMapper;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.ProjectResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.service.IProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<ProjectResponse>> getAdminProjects(Pageable pageable) {
        Page<ProjectResponse> page = projectRepository.findAll(pageable).map(projectMapper::toResponse);
        return ApiResponse.success(PageResult.of(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ProjectResponse> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(project -> ApiResponse.success(projectMapper.toResponse(project)))
                .orElseThrow(() -> new BusinessException(404, "Project not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    @LogOperation("Create Project")
    public ApiResponse<ProjectResponse> createProject(ProjectRequest request) {
        Project project = projectMapper.toEntity(request);
        projectRepository.save(project);
        log.info("Project created: {}", project.getName());
        return ApiResponse.success("Project created successfully", projectMapper.toResponse(project));
    }

    @Override
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    @LogOperation("Update Project")
    public ApiResponse<ProjectResponse> updateProject(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Project not found"));

        project.setName(request.name());
        project.setDescription(request.description());
        project.setCoverImage(request.coverImage());
        project.setGithubUrl(request.githubUrl());
        project.setPreviewUrl(request.previewUrl());
        project.setTechStack(request.techStack());
        project.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        project.setIsPublished(request.isPublished());

        projectRepository.save(project);
        log.info("Project updated: {}", project.getName());
        return ApiResponse.success("Project updated successfully", projectMapper.toResponse(project));
    }

    @Override
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    @LogOperation("Delete Project")
    public ApiResponse<Void> deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new BusinessException(404, "Project not found");
        }
        projectRepository.deleteById(id);
        log.info("Project deleted id: {}", id);
        return ApiResponse.success("Project deleted successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "'public_list'")
    public ApiResponse<List<ProjectResponse>> getPublicProjects() {
        List<Project> projects = projectRepository.findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();
        return ApiResponse.success(projectMapper.toResponseList(projects));
    }
}
