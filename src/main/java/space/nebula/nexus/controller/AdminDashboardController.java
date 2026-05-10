package space.nebula.nexus.controller;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.DashboardStatsResponse;
import space.nebula.nexus.service.IDashboardService;

@Tag(name = "Admin Dashboard", description = "Admin dashboard statistics APIs")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IDashboardService dashboardService;

    @Operation(summary = "Get Dashboard Statistics", description = "Retrieve overall statistics for the admin dashboard")
    @PreAuthorize("hasAnyAuthority('admin', 'dashboard:view')")
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStatistics() {
        return dashboardService.getStatistics();
    }
}
