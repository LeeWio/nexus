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
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.service.AssetUsageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedAssetsCleanupTask {

	private final FileRepository fileRepository;
	private final AssetUsageService assetUsageService;
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
		List<FileMetadata> potentialOrphans = fileRepository.findByIsDeletedFalseAndCreatedAtBefore(cutoff);

		if (potentialOrphans.isEmpty()) {
			log.info("Cleanup completed. No potential abandoned media assets were found.");
			return;
		}

		// Reuse the same reference rules as manual deletion without querying per file.
		var usage = assetUsageService.loadSnapshot();

		long purgedCount = 0;
		long spaceSavedBytes = 0;

		for (FileMetadata file : potentialOrphans) {
			String fileName = file.getFileName();

			if (!usage.isInUse(file)) {
				try {
					// Delete metadata in the transaction first. Physical objects are removed only
					// after commit so a database rollback cannot leave a live row without a file.
					fileRepository.delete(file);
					List<String> filesToDelete = new ArrayList<>();
					filesToDelete.add(fileName);

					String thumbnailName = AssetUsageService.thumbnailObjectName(file);
					if (thumbnailName != null) {
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
