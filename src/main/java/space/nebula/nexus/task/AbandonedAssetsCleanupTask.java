package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedAssetsCleanupTask
{

	private final FileRepository fileRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final StorageProvider storageProvider;

	/**
	 * Automatically purges orphaned files (uploaded but never referenced) daily at 2:00 AM.
	 * Assets must be older than 24 hours to prevent cleaning files currently being drafted.
	 */
	@Scheduled(cron = "0 0 2 * * ?")
	@Transactional
	public void cleanAbandonedAssets()
	{
		log.info("Starting scheduled cleanup of abandoned media assets...");
		LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
		List<FileMetadata> potentialOrphans = fileRepository.findByCreatedAtBefore(cutoff);

		long purgedCount = 0;
		long spaceSavedBytes = 0;

		for (FileMetadata file : potentialOrphans)
		{
			String fileName = file.getFileName();
			
			// Verify if the file unique stored name is referenced anywhere in active database fields
			boolean referencedInPosts = postRepository.existsByContentContaining(fileName) 
					|| postRepository.existsBySummaryContaining(fileName);
			boolean referencedInComments = commentRepository.existsByContentContaining(fileName);
			boolean referencedInAvatars = userRepository.existsByAvatarContaining(fileName);
			boolean referencedInProjects = projectRepository.existsByCoverImageContaining(fileName);

			if (!referencedInPosts && !referencedInComments && !referencedInAvatars && !referencedInProjects)
			{
				try
				{
					// 1. Delete physical file from storage
					storageProvider.delete(fileName);

					// 2. Delete thumbnail if present
					if (file.getThumbnailUrl() != null)
					{
						String thumbnailName = "thumb_" + fileName;
						storageProvider.delete(thumbnailName);
					}

					// 3. Purge metadata row
					fileRepository.delete(file);
					
					purgedCount++;
					spaceSavedBytes += file.getFileSize();
					log.info("Successfully purged abandoned media asset: {} (Size: {} bytes)", fileName, file.getFileSize());
				}
				catch (Exception e)
				{
					log.error("Failed to delete abandoned file from storage: {}", fileName, e);
				}
			}
		}

		if (purgedCount > 0)
		{
			double spaceSavedMb = (double) spaceSavedBytes / (1024 * 1024);
			log.info("Cleanup completed! Purged {} orphaned media files, freeing up {:.2f} MB of storage space.",
					purgedCount, spaceSavedMb);
		}
		else
		{
			log.info("Cleanup completed. No abandoned media assets were found.");
		}
	}
}
