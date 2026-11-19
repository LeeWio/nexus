package space.nebula.nexus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.payload.response.AssetUsageResponse;
import space.nebula.nexus.service.AssetUsageService;
import space.nebula.nexus.task.AbandonedAssetsCleanupTask;
import space.nebula.nexus.entity.FileMetadata;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({AssetUsageService.class, AbandonedAssetsCleanupTask.class})
class FileRepositoryDataJpaTest {

	@Autowired
	private FileRepository repository;

	@Autowired
	private PostRepository posts;

	@Autowired
	private UserRepository users;

	@Autowired
	private AssetUsageService usage;

	@Autowired
	private AbandonedAssetsCleanupTask cleanup;

	@MockitoBean
	private StorageProvider storageProvider;

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

	@Test
	void articleCoversAndStandaloneThumbnailReferencesSurviveCleanup() {
		var cover = save("protected-cover.png", "Cover", false);
		var thumbnailOwner = save("protected-image.png", "Inline image", false);
		thumbnailOwner.setThumbnailUrl("/files/thumb_protected-image.jpg?version=2");
		var unused = save("unused-asset.png", "Unused", false);
		var now = LocalDateTime.now().minusDays(3);
		repository.saveAndFlush(thumbnailOwner);

		var writer = new User();
		writer.setUsername("asset-coverage-writer");
		writer.setEmail("asset-writer@example.test");
		writer.setPassword("test");
		writer.setCreatedAt(now);
		users.save(writer);

		var post = new Post();
		post.setAuthor(writer);
		post.setTitle("Cover-only asset");
		post.setSlug("cover-only-asset");
		post.setCoverImage("/files/" + cover.getFileName());
		post.setContent("![Inline](/files/thumb_protected-image.jpg)");
		post.setContentType(PostContentType.MDX);
		post.setStatus(PostStatus.PUBLISHED);
		post.setCreatedAt(now);
		posts.saveAndFlush(post);

		assertEquals(java.util.List.of(
				new AssetUsageResponse.Location(AssetUsageResponse.Kind.ARTICLE_COVER, post.getId(), post.getTitle())),
				usage.getUsage(cover).locations());
		assertEquals(java.util.List.of(new AssetUsageResponse.Location(AssetUsageResponse.Kind.ARTICLE_CONTENT,
				post.getId(), post.getTitle())), usage.getUsage(thumbnailOwner).locations());
		assertTrue(usage.isInUse(cover));

		cleanup.cleanAbandonedAssets();

		assertTrue(repository.findById(cover.getId()).isPresent());
		assertTrue(repository.findById(thumbnailOwner.getId()).isPresent());
		assertTrue(repository.findById(unused.getId()).isEmpty());
	}

	private FileMetadata save(String name, String originalName, boolean deleted) {
		var file = new FileMetadata();
		file.setFileName(name);
		file.setOriginalName(originalName);
		file.setFileUrl("/files/" + name);
		file.setFileType("application/pdf");
		file.setFileSize(10L);
		file.setIsDeleted(deleted);
		var time = LocalDateTime.now().minusDays(3);
		file.setCreatedAt(time);
		file.setUpdatedAt(time);
		return repository.save(file);
	}
}
