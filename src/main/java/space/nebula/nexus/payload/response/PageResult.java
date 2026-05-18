package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

/**
 * Unified response wrapper for paginated data.
 * @param <T> The type of the data list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated data wrapper")
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "List of data items for current page")
    private List<T> list;

    @Schema(description = "Total number of items matching filter", example = "100")
    private long total;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page;

    @Schema(description = "Number of items per page", example = "10")
    private int size;

    @Schema(description = "Total number of pages", example = "10")
    private int totalPages;

    /**
     * Converts a Spring Data Page object to a PageResult.
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1, // Convert 0-based to 1-based for frontend
                page.getSize(),
                page.getTotalPages()
        );
    }
}
