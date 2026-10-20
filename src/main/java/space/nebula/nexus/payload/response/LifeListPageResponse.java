package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Filtered life-list page plus the overview counts for the same audience")
public record LifeListPageResponse(
		List<LifeListItemResponse> list,
		long total,
		int page,
		int size,
		int totalPages,
		LifeListSummaryResponse summary) {
}
