package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ILinkHealthService;

@Tag(name = "Admin Link Health Management", description = "Endpoints for monitoring and checking external link health")
@RestController
@RequestMapping("/api/v1/admin/links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLinkHealthController {

    private final ILinkHealthService linkHealthService;

    @GetMapping("/broken")
    @Operation(summary = "Get broken links", description = "Retrieve a paginated list of all dead links found in the system.")
    public ApiResponse<PageResult<LinkCheckLog>> getBrokenLinks(@PageableDefault(size = 20) Pageable pageable) {
        return linkHealthService.getBrokenLinks(pageable);
    }

    @PostMapping("/scan")
    @Operation(summary = "Trigger manual scan", description = "Manually initiate a full system scan for external link health.")
    public ApiResponse<Void> triggerManualScan() {
        linkHealthService.runFullScan();
        return ApiResponse.success("Link health check scan initiated in background", null);
    }

    @DeleteMapping("/logs")
    @Operation(summary = "Clear check logs", description = "Permanently clear all health check history.")
    public ApiResponse<Void> clearLogs() {
        return linkHealthService.clearLogs();
    }
}
