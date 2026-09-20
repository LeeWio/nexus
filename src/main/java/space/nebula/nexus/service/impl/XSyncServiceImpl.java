package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.config.XProperties;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.MomentMedia;
import space.nebula.nexus.entity.MomentXSync;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.MomentXSyncStatus;
import space.nebula.nexus.integration.x.XClient;
import space.nebula.nexus.payload.response.MomentXSyncResponse;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.MomentXSyncRepository;
import space.nebula.nexus.service.IXSyncService;
import space.nebula.nexus.utils.MomentContentPolicy;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class XSyncServiceImpl implements IXSyncService {

	private final MomentXSyncRepository momentXSyncRepository;
	private final MomentRepository momentRepository;
	private final XProperties xProperties;
	private final XClient xClient;
	private final StorageProvider storageProvider;

	@Override
	@Transactional
	public boolean enqueueOnCreate(Moment moment, Boolean shareToX) {
		if (!Boolean.TRUE.equals(shareToX)) {
			return false;
		}
		Assert.isTrue(moment.getVisibility() == MomentVisibility.PUBLIC,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only public moments can be shared to X"));
		if (!xProperties.isConfigured()) {
			log.info("shareToX requested for moment {} but X sync is disabled or incomplete; skipping", moment.getId());
			return false;
		}
		if (momentXSyncRepository.findByMomentId(moment.getId()).isPresent()) {
			return false;
		}

		MomentXSync sync = new MomentXSync();
		sync.setMoment(moment);
		sync.setStatus(MomentXSyncStatus.PENDING);
		sync.setAttempts(0);
		momentXSyncRepository.save(sync);
		return true;
	}

	@Override
	@Transactional
	public void processPending(Long momentId) {
		MomentXSync sync = momentXSyncRepository.findByMomentId(momentId).orElse(null);
		if (sync == null) {
			return;
		}
		if (sync.getStatus() == MomentXSyncStatus.POSTED || sync.getStatus() == MomentXSyncStatus.SKIPPED) {
			return;
		}
		if (!xProperties.isConfigured()) {
			markFailed(sync, "X sync is disabled or incomplete");
			return;
		}

		Moment moment = momentRepository.findById(momentId).orElse(null);
		if (moment == null) {
			markFailed(sync, "Moment not found");
			return;
		}
		if (moment.getVisibility() != MomentVisibility.PUBLIC) {
			sync.setStatus(MomentXSyncStatus.SKIPPED);
			sync.setLastError("Moment is no longer public");
			momentXSyncRepository.save(sync);
			return;
		}

		sync.setAttempts(sync.getAttempts() == null ? 1 : sync.getAttempts() + 1);
		try {
			String text = MomentContentPolicy.truncateForX(MomentContentPolicy.visibleText(moment.getContent()),
					xProperties.getMaxTextLength());
			List<String> mediaIds = uploadImages(moment);
			if ((text == null || text.isBlank()) && mediaIds.isEmpty()) {
				markFailed(sync, "Nothing to post: empty text and no uploadable images");
				return;
			}

			XClient.CreatedPost created = xClient.createPost(text, mediaIds);
			sync.setStatus(MomentXSyncStatus.POSTED);
			sync.setXPostId(created.id());
			sync.setXPostUrl(created.url());
			sync.setPostedAt(LocalDateTime.now());
			sync.setLastError(null);
			momentXSyncRepository.save(sync);
			log.info("Moment {} posted to X as {}", momentId, created.id());
		} catch (Exception e) {
			log.warn("Failed to post moment {} to X: {}", momentId, e.getMessage());
			markFailed(sync, e.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public MomentXSyncResponse findByMomentId(Long momentId) {
		return momentXSyncRepository.findByMomentId(momentId).map(this::toResponse).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, MomentXSyncResponse> findByMomentIds(Collection<Long> momentIds) {
		if (momentIds == null || momentIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, MomentXSyncResponse> result = new HashMap<>();
		for (MomentXSync sync : momentXSyncRepository.findByMomentIdIn(momentIds)) {
			if (sync.getMoment() != null && sync.getMoment().getId() != null) {
				result.put(sync.getMoment().getId(), toResponse(sync));
			}
		}
		return result;
	}

	private List<String> uploadImages(Moment moment) {
		if (moment.getImages() == null || moment.getImages().isEmpty()) {
			return List.of();
		}
		List<MomentMedia> ordered = moment.getImages().stream()
				.sorted(Comparator.comparing(MomentMedia::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
				.limit(Math.max(0, xProperties.getMaxImages())).toList();

		List<String> mediaIds = new ArrayList<>();
		for (MomentMedia media : ordered) {
			FileMetadata file = media.getFile();
			if (file == null || file.getFileName() == null) {
				continue;
			}
			String type = file.getFileType();
			if (type == null || !type.startsWith("image/") || type.contains("gif")) {
				// v1: static images only
				continue;
			}
			try (InputStream in = storageProvider.open(file.getFileName())) {
				byte[] bytes = in.readAllBytes();
				if (bytes.length == 0) {
					continue;
				}
				mediaIds.add(xClient.uploadImage(bytes, type, file.getOriginalName()));
			} catch (Exception e) {
				throw new IllegalStateException("Failed to upload moment image " + file.getId() + ": " + e.getMessage(),
						e);
			}
		}
		return mediaIds;
	}

	private void markFailed(MomentXSync sync, String error) {
		sync.setStatus(MomentXSyncStatus.FAILED);
		sync.setLastError(truncateError(error));
		momentXSyncRepository.save(sync);
	}

	private MomentXSyncResponse toResponse(MomentXSync sync) {
		return new MomentXSyncResponse(sync.getStatus(), sync.getXPostId(), sync.getXPostUrl(), sync.getLastError(),
				sync.getAttempts(), sync.getPostedAt());
	}

	private static String truncateError(String error) {
		if (error == null) {
			return null;
		}
		String cleaned = error.replace('\n', ' ').trim();
		return cleaned.length() <= 1000 ? cleaned : cleaned.substring(0, 1000);
	}
}
