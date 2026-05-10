package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.PublicStatsResponse;
import space.nebula.nexus.service.IDashboardService;

@Tag(name = "Public Statistics", description = "Public endpoints for site-wide statistics")
@RestController
@RequestMapping("/api/v1/public/stats")
@RequiredArgsConstructor
public class PublicStatsController {

    private final IDashboardService dashboardService;

    @Operation(summary = "Get site statistics", description = "Returns non-sensitive site metrics for the front-end dashboard")
    @GetMapping
    public ApiResponse<PublicStatsResponse> getPublicStatistics() {
        return dashboardService.getPublicStatistics();
    }
}
