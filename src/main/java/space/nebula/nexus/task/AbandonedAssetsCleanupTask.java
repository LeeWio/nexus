package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedAssetsCleanupTask {

	private final FileRepository fileRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final StorageProvider storageProvider;

	/**
	 * Automatically purges orphaned files (uploaded but never referenced) daily at
	 * 2:00 AM. Assets must be older than 24 hours to prevent cleaning files
	 * currently being drafted.
	 */
	@Scheduled(cron = "0 0 2 * * ?")
	@SchedulerLock(name = "abandonedAssetsCleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
	@Transactional
	public void cleanAbandonedAssets() {
		log.info("Starting scheduled cleanup of abandoned media assets...");
		LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
		List<FileMetadata> potentialOrphans = fileRepository.findByCreatedAtBefore(cutoff);

		if (potentialOrphans.isEmpty()) {
			log.info("Cleanup completed. No potential abandoned media assets were found.");
			return;
		}

		// Preload all text content fields once to avoid N+1 full-table wildcard SELECT
		// queries
		log.info("Preloading database content for in-memory asset reference tracking...");
		List<String> postContents = postRepository.findAllContents();
		List<String> postSummaries = postRepository.findAllSummaries();
		List<String> commentContents = commentRepository.findAllContents();
		List<String> userAvatars = userRepository.findAllAvatars();
		List<String> projectCovers = projectRepository.findAllCoverImages();

		long purgedCount = 0;
		long spaceSavedBytes = 0;

		for (FileMetadata file : potentialOrphans) {
			String fileName = file.getFileName();

			// Verify if the file unique stored name is referenced anywhere in preloaded
			// strings
			boolean referencedInPosts = postContents.stream().anyMatch(content -> content.contains(fileName))
					|| postSummaries.stream().anyMatch(summary -> summary.contains(fileName));
			boolean referencedInComments = commentContents.stream().anyMatch(content -> content.contains(fileName));
			boolean referencedInAvatars = userAvatars.stream().anyMatch(avatar -> avatar.contains(fileName));
			boolean referencedInProjects = projectCovers.stream().anyMatch(cover -> cover.contains(fileName));
			boolean referencedInMoments = fileRepository.isReferencedByMoment(file.getId());

			if (!referencedInPosts && !referencedInComments && !referencedInAvatars && !referencedInProjects
					&& !referencedInMoments) {
				try {
					// Delete metadata in the transaction first. Physical objects are removed only
					// after commit so a database rollback cannot leave a live row without a file.
					fileRepository.delete(file);
					List<String> filesToDelete = new ArrayList<>();
					filesToDelete.add(fileName);

					// Delete thumbnail if present
					if (file.getThumbnailUrl() != null) {
						String thumbnailName = "thumb_" + fileName;
						filesToDelete.add(thumbnailName);
					}
					registerStorageDeletionAfterCommit(filesToDelete);

					purgedCount++;
					spaceSavedBytes += file.getFileSize();
					log.info("Successfully purged abandoned media asset: {} (Size: {} bytes)", fileName,
							file.getFileSize());
				} catch (Exception e) {
					log.error("Failed to delete abandoned file from storage: {}", fileName, e);
				}
			}
		}

		if (purgedCount > 0) {
			double spaceSavedMb = (double) spaceSavedBytes / (1024 * 1024);
			log.info("Cleanup completed! Purged {} orphaned media files, freeing up {:.2f} MB of storage space.",
					purgedCount, spaceSavedMb);
		} else {
			log.info("Cleanup completed. No abandoned media assets were found.");
		}
	}

	private void registerStorageDeletionAfterCommit(List<String> fileNames) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteStorageObjects(fileNames);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteStorageObjects(fileNames);
			}
		});
	}

	private void deleteStorageObjects(List<String> fileNames) {
		for (String fileName : fileNames) {
			try {
				storageProvider.delete(fileName);
			} catch (RuntimeException e) {
				log.error("Failed to delete committed storage object {}", fileName, e);
			}
		}
	}
}
