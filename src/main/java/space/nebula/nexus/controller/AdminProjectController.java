package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.ProjectRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.ProjectResponse;
import space.nebula.nexus.service.IProjectService;

/**
 * Controller for managing portfolio projects. Handles CRUD operations for
 * showcase projects with metadata and GitHub integration.
 */
@Tag(name = "Admin Project Management", description = "Endpoints for managing portfolio projects and showcases")
@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {

	private final IProjectService projectService;

	@GetMapping
	@Operation(summary = "Get all projects", description = "Retrieve a paginated list of all projects for administrative management.")
	public ApiResponse<PageResult<ProjectResponse>> getAllProjects(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10) Pageable pageable) {
		return projectService.getAdminProjects(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get project by ID", description = "Fetch detailed information for a specific project.")
	public ApiResponse<ProjectResponse> getProjectById(@Parameter(description = "Project ID") @PathVariable Long id) {
		return projectService.getProjectById(id);
	}

	@PostMapping
	@Operation(summary = "Create project", description = "Add a new project to the portfolio.")
	public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
		return projectService.createProject(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update project", description = "Modify an existing project's details, tech stack, or visibility.")
	public ApiResponse<ProjectResponse> updateProject(@Parameter(description = "Project ID") @PathVariable Long id,
			@Valid @RequestBody ProjectRequest request) {
		return projectService.updateProject(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete project", description = "Permanently remove a project from the portfolio.")
	public ApiResponse<Void> deleteProject(@Parameter(description = "Project ID") @PathVariable Long id) {
		return projectService.deleteProject(id);
	}
}
