package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MomentBookmarkImportRequest(@NotNull @Size(min = 1, max = 100) List<@NotNull @Positive Long> ids) {
}
