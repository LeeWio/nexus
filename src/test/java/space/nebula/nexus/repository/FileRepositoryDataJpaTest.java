package space.nebula.nexus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.FileMetadata;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class FileRepositoryDataJpaTest {

	@Autowired
	private FileRepository repository;

	@Test
	void listsAndSearchesOnlyActiveAssets() {
		save("first.pdf", "Report%.pdf", false);
		var newest = save("REPORT%.pdf", "second.pdf", false);
		save("report%deleted.pdf", "Report% old.pdf", true);
		save("other.pdf", "Report draft.pdf", false);

		var firstPage = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"));
		assertEquals(3, repository.findByIsDeletedFalse(firstPage).getTotalElements());
		var matching = repository
				.findByIsDeletedFalseAndOriginalNameContainingIgnoreCaseOrIsDeletedFalseAndFileNameContainingIgnoreCase(
						"report%", "report%", firstPage);
		assertEquals(2, matching.getTotalElements());
		assertEquals(newest.getId(), matching.getContent().getFirst().getId());
	}

	private FileMetadata save(String name, String originalName, boolean deleted) {
		var file = new FileMetadata();
		file.setFileName(name);
		file.setOriginalName(originalName);
		file.setFileUrl("/files/" + name);
		file.setFileType("application/pdf");
		file.setFileSize(10L);
		file.setIsDeleted(deleted);
		var time = LocalDateTime.of(2026, 11, 19, 12, 0);
		file.setCreatedAt(time);
		file.setUpdatedAt(time);
		return repository.save(file);
	}
}
