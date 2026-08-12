package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ColumnResponse;
import space.nebula.nexus.service.IPostSeriesService;

import java.util.List;

/** Public, reader-oriented access to published editorial columns. */
@Tag(name = "Public Columns", description = "Browse published editorial columns and their articles")
@RestController
@RequestMapping("/api/v1/public/columns")
@RequiredArgsConstructor
public class PublicColumnController {

	private final IPostSeriesService seriesService;

	@GetMapping
	@Operation(summary = "List published columns")
	public ApiResponse<List<ColumnResponse>> listColumns() {
		return seriesService.retrievePublicColumns();
	}

	@GetMapping("/{slug}")
	@Operation(summary = "Get a published column and its articles")
	public ApiResponse<ColumnResponse> getColumn(
			@Parameter(description = "Column URL slug") @PathVariable String slug) {
		return seriesService.retrievePublicColumn(slug);
	}
}
