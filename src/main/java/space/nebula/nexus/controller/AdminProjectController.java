package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Project Management", description = "Endpoints for managing portfolio projects")
public class AdminProjectController {

    private final IProjectService projectService;

    @GetMapping
    @Operation(summary = "Get all projects (paginated)")
    public ApiResponse<PageResult<ProjectResponse>> getAllProjects(@PageableDefault(size = 10) Pageable pageable) {
        return projectService.getAdminProjects(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ApiResponse<ProjectResponse> getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project")
    public ApiResponse<ProjectResponse> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        return projectService.deleteProject(id);
    }
}
