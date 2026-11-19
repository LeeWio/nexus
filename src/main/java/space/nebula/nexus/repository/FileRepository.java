package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FileMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long>, JpaSpecificationExecutor<FileMetadata> {

	@EntityGraph(attributePaths = {"uploader"})
	Optional<FileMetadata> findByFileName(String fileName);

	Optional<FileMetadata> findFirstByFileHash(String fileHash);

	List<FileMetadata> findByIsDeletedFalseAndCreatedAtBefore(LocalDateTime cutoff);

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
	Page<FileMetadata> findByIsDeletedFalseAndOriginalNameContainingIgnoreCaseOrIsDeletedFalseAndFileNameContainingIgnoreCase(
			String originalName, String fileName, Pageable pageable);

	Page<FileMetadata> findByIsDeletedFalse(Pageable pageable);

	@Query("""
			select media.file.id as fileId, media.moment.id as momentId
			from MomentMedia media where :fileId is null or media.file.id = :fileId
			order by media.moment.id
			""")
	List<MomentAssetReference> findMomentAssetReferences(Long fileId);

	interface MomentAssetReference {
		Long getFileId();

		Long getMomentId();
	}
}
