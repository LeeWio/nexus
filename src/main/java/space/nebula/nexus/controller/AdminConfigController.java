package space.nebula.nexus.controller;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.service.IConfigService;

import java.util.List;

@Tag(name = "Admin System Config", description = "System configuration management APIs")
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
public class AdminConfigController {

    private final IConfigService configService;

    @Operation(summary = "Get All Configs")
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping
    public ApiResponse<List<ConfigResponse>> getAllConfigs() {
        return configService.getAllConfigs();
    }

    @Operation(summary = "Create Config")
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping
    public ApiResponse<ConfigResponse> createConfig(@Valid @RequestBody ConfigRequest request) {
        return configService.createConfig(request);
    }

    @Operation(summary = "Update Config")
    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/{id}")
    public ApiResponse<ConfigResponse> updateConfig(@PathVariable Long id, @Valid @RequestBody ConfigRequest request) {
        return configService.updateConfig(id, request);
    }

    @Operation(summary = "Delete Config")
    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        return configService.deleteConfig(id);
    }
}
