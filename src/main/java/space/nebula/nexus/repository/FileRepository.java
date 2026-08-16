package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FileMetadata;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long>, JpaSpecificationExecutor<FileMetadata> {

	@EntityGraph(attributePaths = {"uploader"})
	Optional<FileMetadata> findByFileName(String fileName);

	Optional<FileMetadata> findByFileHash(String fileHash);

	java.util.List<FileMetadata> findByCreatedAtBefore(java.time.LocalDateTime cutoff);

	@Query("""
			select count(file) as assetCount, coalesce(sum(file.fileSize), 0) as logicalBytes,
			       coalesce(sum(file.referenceCount), 0) as totalReferences,
			       min(file.createdAt) as oldestAssetAt, max(file.createdAt) as newestAssetAt
			from FileMetadata file
			where file.isDeleted = false
			""")
	StorageInventoryProjection summarizeStorageInventory();

	interface StorageInventoryProjection {
		Long getAssetCount();

		Long getLogicalBytes();

		Long getTotalReferences();

		LocalDateTime getOldestAssetAt();

		LocalDateTime getNewestAssetAt();
	}

	@Override
	@EntityGraph(attributePaths = {"uploader"})
	Page<FileMetadata> findAll(@Nullable Specification<FileMetadata> spec, Pageable pageable);

	@EntityGraph(attributePaths = {"uploader"})
	Page<FileMetadata> findByOriginalNameContainingIgnoreCaseOrFileNameContainingIgnoreCase(String originalName,
			String fileName, Pageable pageable);

	Page<FileMetadata> findByIsDeletedFalse(Pageable pageable);

	@Query("select (count(media) > 0) from MomentMedia media where media.file.id = :fileId")
	boolean isReferencedByMoment(@Param("fileId") Long fileId);
}
