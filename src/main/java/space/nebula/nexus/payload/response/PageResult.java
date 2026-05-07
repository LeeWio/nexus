package space.nebula.nexus.payload.response;

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
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private int page;
    private int size;
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
