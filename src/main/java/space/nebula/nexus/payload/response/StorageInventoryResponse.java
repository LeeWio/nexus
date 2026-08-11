package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "Aggregate metadata used to verify storage and backup coverage")
public record StorageInventoryResponse(
		@Schema(description = "Configured storage provider", example = "local") String providerType,
		@Schema(description = "Number of active file metadata records", example = "42") long assetCount,
		@Schema(description = "Total stored asset bytes recorded in metadata", example = "1048576") long logicalBytes,
		@Schema(description = "Total references across deduplicated assets", example = "57") long totalReferences,
		@Schema(description = "Creation time of the oldest active asset") LocalDateTime oldestAssetAt,
		@Schema(description = "Creation time of the newest active asset") LocalDateTime newestAssetAt)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;
}
