package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.SeriesRequest;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.service.IPostSeriesService;

import java.util.List;

/**
 * Controller for administrative blog post series management.
 * Provides endpoints for organizing posts into thematic series or columns.
 */
@Tag(name = "Admin Post Series Management", description = "Endpoints for managing blog post series and thematic columns")
@RestController
@RequestMapping("/api/v1/admin/series")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostSeriesController {

    private final IPostSeriesService seriesService;

    @GetMapping
    @Operation(summary = "Retrieve all series", description = "Fetch a complete list of all post series for administrative management.")
    public ApiResponse<List<SeriesResponse>> retrieveAllSeries() {
        return seriesService.retrieveAllSeriesForAdmin();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve series by ID", description = "Fetch detailed information for a specific post series.")
    public ApiResponse<SeriesResponse> retrieveSeries(
            @Parameter(description = "Series ID") @PathVariable Long id) {
        return seriesService.retrieveSeriesById(id);
    }

    @PostMapping
    @Operation(summary = "Create post series", description = "Add a new series to organize related blog posts.")
    public ApiResponse<SeriesResponse> createSeries(@Valid @RequestBody SeriesRequest request) {
        return seriesService.createSeries(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update post series", description = "Modify an existing series' metadata, name, or slug.")
    public ApiResponse<SeriesResponse> updateSeries(
            @Parameter(description = "Series ID") @PathVariable Long id, 
            @Valid @RequestBody SeriesRequest request) {
        return seriesService.updateSeries(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post series", description = "Permanently remove a post series. Associated posts will be unlinked.")
    public ApiResponse<Void> deleteSeries(@Parameter(description = "Series ID") @PathVariable Long id) {
        return seriesService.deleteSeries(id);
    }
}
