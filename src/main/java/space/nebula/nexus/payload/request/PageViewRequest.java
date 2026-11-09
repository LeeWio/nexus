package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A real browser navigation reported by the site, not an API prefetch. */
@Schema(description = "First-party page view")
public record PageViewRequest(
		@NotBlank @Size(max = 255) @Schema(description = "Site path without query string", example = "/single/me-and-machines") String path,
		@Size(max = 500) @Schema(description = "document.referrer, possibly empty") String referrer,
		@Size(max = 160) @Schema(description = "Document title at navigation time") String title) {
}
