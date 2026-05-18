package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.service.IPostSeriesService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/series")
@RequiredArgsConstructor
@Tag(name = "Public Post Series", description = "Public endpoints for exploring post series/columns")
public class PublicPostSeriesController {

    private final IPostSeriesService seriesService;

    @GetMapping
    @Operation(summary = "Retrieve all published series")
    public ApiResponse<List<SeriesResponse>> retrievePublicSeries() {
        return seriesService.retrievePublicSeriesList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Retrieve a specific series with its ordered posts")
    public ApiResponse<SeriesResponse> retrieveSeriesWithPosts(@PathVariable String slug) {
        return seriesService.retrieveSeriesWithPosts(slug);
    }
}
