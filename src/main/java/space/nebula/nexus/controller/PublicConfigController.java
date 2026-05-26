package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.service.IConfigService;

import java.util.List;

/**
 * Controller for public access to system configurations.
 */
@Tag(name = "Public System Config", description = "Public configuration APIs for frontend applications")
@RestController
@RequestMapping("/api/v1/public/configs")
@RequiredArgsConstructor
public class PublicConfigController
{

	private final IConfigService configService;

	@Operation(summary = "Get Public Configs", description = "Retrieve all system configurations marked as public.")
	@GetMapping
	public ApiResponse<List<ConfigResponse>> getPublicConfigs()
	{
		return configService.getPublicConfigs();
	}

	@Operation(summary = "Get Public Config by Key", description = "Retrieve a specific public configuration by its unique key.")
	@GetMapping("/{key}")
	public ApiResponse<ConfigResponse> getConfigByKey(@PathVariable String key)
	{
		ApiResponse<ConfigResponse> response = configService.getConfigByKey(key);
		if (response.data() != null && Boolean.TRUE.equals(response.data().getIsPublic()))
		{
			return response;
		}
		return ApiResponse.error(404, "Configuration not found or not authorized for public access");
	}
}
