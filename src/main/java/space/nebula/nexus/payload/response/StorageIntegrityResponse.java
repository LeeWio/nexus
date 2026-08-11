package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema(description = "A page of active storage assets verified against the configured storage provider")
public record StorageIntegrityResponse(
		@Schema(description = "Configured storage provider", example = "local") String providerType,
		@Schema(description = "Number of active assets checked in this response", example = "100") long checkedAssetCount,
		@Schema(description = "Number of missing objects found in this response", example = "2") long missingObjectCount,
		@Schema(description = "Total number of active assets available for verification", example = "420") long totalActiveAssetCount,
		@Schema(description = "Current response page number (1-based)", example = "1") int page,
		@Schema(description = "Number of assets checked per response", example = "100") int size,
		@Schema(description = "Total verification pages", example = "5") int totalPages,
		@Schema(description = "Objects recorded in metadata but absent from storage") List<MissingObject> missingObjects)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "A missing storage object associated with an active asset")
	public record MissingObject(@Schema(description = "File metadata ID", example = "42") Long assetId,
			@Schema(description = "Missing object kind", example = "original") String objectKind)
			implements
				Serializable {
		private static final long serialVersionUID = 1L;
	}
}
