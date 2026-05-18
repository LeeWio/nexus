package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/admin/series")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Post Series Management", description = "Endpoints for managing blog post series/columns")
public class AdminPostSeriesController {

    private final IPostSeriesService seriesService;

    @GetMapping
    @Operation(summary = "Retrieve all series for management")
    public ApiResponse<List<SeriesResponse>> retrieveAllSeries() {
        return seriesService.retrieveAllSeriesForAdmin();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve series details by ID")
    public ApiResponse<SeriesResponse> retrieveSeries(@PathVariable Long id) {
        return seriesService.retrieveSeriesById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new post series")
    public ApiResponse<SeriesResponse> createSeries(@Valid @RequestBody SeriesRequest request) {
        return seriesService.createSeries(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post series")
    public ApiResponse<SeriesResponse> updateSeries(@PathVariable Long id, @Valid @RequestBody SeriesRequest request) {
        return seriesService.updateSeries(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post series")
    public ApiResponse<Void> deleteSeries(@PathVariable Long id) {
        return seriesService.deleteSeries(id);
    }
}
