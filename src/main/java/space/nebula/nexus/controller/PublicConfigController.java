package space.nebula.nexus.controller;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.service.IConfigService;

import java.util.List;

@Tag(name = "Public System Config", description = "Public configuration APIs for frontend")
@RestController
@RequestMapping("/api/v1/public/configs")
@RequiredArgsConstructor
public class PublicConfigController {

    private final IConfigService configService;

    @Operation(summary = "Get Public Configs", description = "Retrieve all public system configurations")
    @GetMapping
    public ApiResponse<List<ConfigResponse>> getPublicConfigs() {
        return configService.getPublicConfigs();
    }

    @Operation(summary = "Get Public Config by Key", description = "Retrieve a specific configuration by its key")
    @GetMapping("/{key}")
    public ApiResponse<ConfigResponse> getConfigByKey(@PathVariable String key) {
        // Warning: This endpoint exposes any config by key if the key is known.
        // It's generally better to let the frontend just fetch all public configs.
        // However, if we need it, we should ensure we only return it if it is public.
        ApiResponse<ConfigResponse> response = configService.getConfigByKey(key);
        if (response.getData() != null && Boolean.TRUE.equals(response.getData().getIsPublic())) {
             return response;
        }
        return ApiResponse.error(404, "Config not found or is not public");
    }
}
