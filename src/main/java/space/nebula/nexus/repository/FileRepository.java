package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FileMetadata;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long>, JpaSpecificationExecutor<FileMetadata> {

	@EntityGraph(attributePaths = {"uploader"})
	Optional<FileMetadata> findByFileName(String fileName);

	@Override
	@EntityGraph(attributePaths = {"uploader"})
	Page<FileMetadata> findAll(@Nullable Specification<FileMetadata> spec, Pageable pageable);
}
