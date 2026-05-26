package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ProjectResponse;
import space.nebula.nexus.service.IProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/projects")
@RequiredArgsConstructor
@Tag(name = "Public Projects", description = "Public endpoints for portfolio projects")
public class PublicProjectController
{

	private final IProjectService projectService;

	@GetMapping
	@Operation(summary = "Get published projects", description = "Returns all published projects sorted by priority and date")
	public ApiResponse<List<ProjectResponse>> getPublicProjects()
	{
		return projectService.getPublicProjects();
	}
}
