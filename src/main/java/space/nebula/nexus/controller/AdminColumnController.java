package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.SeriesRequest;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.service.IPostSeriesService;

import java.util.List;

/**
 * Administrative API for editorial columns. Columns use the established series
 * persistence model while exposing a reader-facing editorial vocabulary.
 */
@Tag(name = "Admin Columns", description = "Manage editorial columns and their metadata")
@RestController
@RequestMapping("/api/v1/admin/columns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminColumnController {

	private final IPostSeriesService seriesService;

	@GetMapping
	@Operation(summary = "List columns")
	public ApiResponse<List<SeriesResponse>> listColumns() {
		return seriesService.retrieveAllSeriesForAdmin();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a column")
	public ApiResponse<SeriesResponse> getColumn(@PathVariable Long id) {
		return seriesService.retrieveSeriesById(id);
	}

	@PostMapping
	@Operation(summary = "Create a column")
	public ApiResponse<SeriesResponse> createColumn(@Valid @RequestBody SeriesRequest request) {
		return seriesService.createSeries(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a column")
	public ApiResponse<SeriesResponse> updateColumn(@PathVariable Long id, @Valid @RequestBody SeriesRequest request) {
		return seriesService.updateSeries(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a column")
	public ApiResponse<Void> deleteColumn(@PathVariable Long id) {
		return seriesService.deleteSeries(id);
	}
}
