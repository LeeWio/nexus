package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
@Schema(description = "Cursor-based paginated data wrapper")
public record CursorPageResponse<T>(@Schema(description = "Items returned for the current cursor window") List<T> list,

		@Schema(description = "Cursor for the next request. Null when there is no next page.") Long nextCursor,

		@Schema(description = "Whether more items are available after this window") boolean hasMore)
		implements
			Serializable {
}
