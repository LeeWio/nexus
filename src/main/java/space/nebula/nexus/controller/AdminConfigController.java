package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.service.IConfigService;

import java.util.List;

/**
 * Controller for administrative system configuration management. Allows
 * administrators to manage system settings, feature flags, and global
 * constants.
 */
@Tag(name = "Admin System Config", description = "Endpoints for managing system-wide configurations and settings")
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController
{

	private final IConfigService configService;

	@GetMapping
	@Operation(summary = "Get all configs", description = "Retrieve a full list of all system configurations.")
	public ApiResponse<List<ConfigResponse>> getAllConfigs()
	{
		return configService.getAllConfigs();
	}

	@PostMapping
	@Operation(summary = "Create config", description = "Define a new system configuration key-value pair.")
	public ApiResponse<ConfigResponse> createConfig(@Valid @RequestBody ConfigRequest request)
	{
		return configService.createConfig(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update config", description = "Modify an existing system configuration.")
	public ApiResponse<ConfigResponse> updateConfig(@Parameter(description = "Config ID") @PathVariable Long id,
			@Valid @RequestBody ConfigRequest request)
	{
		return configService.updateConfig(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete config", description = "Permanently remove a system configuration.")
	public ApiResponse<Void> deleteConfig(@Parameter(description = "Config ID") @PathVariable Long id)
	{
		return configService.deleteConfig(id);
	}
}
