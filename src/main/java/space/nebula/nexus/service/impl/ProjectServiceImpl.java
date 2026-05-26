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
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.mapper.ProjectMapper;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.ProjectResponse;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.service.IProjectService;

import java.util.List;

import space.nebula.nexus.common.exception.ResourceNotFoundException;

import cn.hutool.core.lang.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService
{

	private final ProjectRepository projectRepository;
	private final ProjectMapper projectMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<ProjectResponse>> getAdminProjects(Pageable pageable)
	{
		Page<ProjectResponse> page = projectRepository.findAll(pageable).map(projectMapper::toResponse);
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ProjectResponse> getProjectById(Long id)
	{
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
		return ApiResponse.success(projectMapper.toResponse(project));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.PROJECTS, allEntries = true)
	@LogOperation("Create Project")
	public ApiResponse<ProjectResponse> createProject(ProjectRequest request)
	{
		Project project = projectMapper.toEntity(request);
		if (project.getSortOrder() == null)
			project.setSortOrder(0);

		projectRepository.save(project);
		log.info("Project created: {}", project.getName());
		return ApiResponse.success("Project created successfully", projectMapper.toResponse(project));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.PROJECTS, allEntries = true)
	@LogOperation("Update Project")
	public ApiResponse<ProjectResponse> updateProject(Long id, ProjectRequest request)
	{
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

		projectMapper.updateEntity(project, request);

		projectRepository.save(project);
		log.info("Project updated: {}", project.getName());
		return ApiResponse.success("Project updated successfully", projectMapper.toResponse(project));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.PROJECTS, allEntries = true)
	@LogOperation("Delete Project")
	public ApiResponse<Void> deleteProject(Long id)
	{
		Assert.isTrue(projectRepository.existsById(id), () -> new ResourceNotFoundException("Project", "id", id));
		projectRepository.deleteById(id);
		log.info("Project deleted id: {}", id);
		return ApiResponse.success("Project deleted successfully", null);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.PROJECTS, key = CacheConstants.PUBLIC_LIST_KEY)
	public ApiResponse<List<ProjectResponse>> getPublicProjects()
	{
		List<Project> projects = projectRepository.findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();
		return ApiResponse.success(projectMapper.toResponseList(projects));
	}
}
