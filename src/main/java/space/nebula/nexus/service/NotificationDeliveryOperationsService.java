package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.entity.NotificationDelivery;
import space.nebula.nexus.entity.NotificationDeliveryRetry;
import space.nebula.nexus.repository.NotificationDeliveryRepository;
import space.nebula.nexus.repository.NotificationDeliveryRetryRepository;
import space.nebula.nexus.payload.request.NotificationDeliveryRetryRequest;
import space.nebula.nexus.payload.response.*;
import space.nebula.nexus.enums.NotificationDeliveryStatus;
import space.nebula.nexus.security.util.SecurityUtil;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryOperationsService {
	private final NotificationDeliveryRepository deliveryRepository;
	private final NotificationDeliveryRetryRepository retryRepository;
	private final NotificationDeliveryService deliveryService;

	@Transactional(readOnly = true)
	public PageResult<NotificationDeliveryResponse> list(NotificationDeliveryStatus status, Long notificationId,
			Pageable pageable) {
		return PageResult.of(deliveryRepository
				.findOperationsPage(status == null ? null : status.name(), notificationId, page(pageable))
				.map(this::toResponse));
	}

	@Transactional(readOnly = true)
	public NotificationDeliveryResponse detail(Long id) {
		return toResponse(find(id));
	}

	@Transactional(readOnly = true)
	public NotificationDeliveryOverviewResponse overview() {
		LocalDateTime now = LocalDateTime.now();
		Map<NotificationDeliveryStatus, Long> counts = new EnumMap<>(NotificationDeliveryStatus.class);
		for (var status : NotificationDeliveryStatus.values())
			counts.put(status, 0L);
		deliveryRepository.countByStatus()
				.forEach(row -> counts.put(NotificationDeliveryStatus.valueOf(row.getStatus()), row.getTotal()));
		long pending = counts.get(NotificationDeliveryStatus.QUEUED) + counts.get(NotificationDeliveryStatus.SENDING)
				+ counts.get(NotificationDeliveryStatus.FAILED);
		return new NotificationDeliveryOverviewResponse(counts, pending, deliveryRepository.countOverdue(now),
				deliveryRepository.oldestPendingAt(), now);
	}

	@Transactional(readOnly = true)
	public PageResult<NotificationDeliveryRetryResponse> retries(Long id, Pageable pageable) {
		find(id);
		return PageResult.of(retryRepository.findByDeliveryId(id, page(pageable))
				.map(row -> new NotificationDeliveryRetryResponse(row.getId(), row.getRequestId(), row.getRequestedBy(),
						row.getReason(), row.getPreviousStatus(), row.getPreviousMaxAttempts(), row.getNewMaxAttempts(),
						row.getAttemptsAtRetry(), row.getCreatedAt())));
	}

	@Transactional
	@LogOperation(value = "Retry Notification Delivery", logArgs = false)
	public NotificationDeliveryResponse retry(Long id, NotificationDeliveryRetryRequest request) {
		String operator = SecurityUtil.getCurrentUsername();
		if (operator == null)
			throw new BusinessException(401, "Authentication required");
		NotificationDelivery delivery = deliveryRepository.findForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", "id", id));
		if (retryRepository.existsByDeliveryIdAndRequestId(id, request.requestId().toString()))
			return toResponse(delivery);
		if (!retryable(delivery))
			throw new BusinessException(409, "Only failed or abandoned deliveries can be retried");
		if (delivery.getAttempts() > Integer.MAX_VALUE - 5)
			throw new BusinessException(409, "Delivery attempt limit reached");
		int budget = Math.max(delivery.getMaxAttempts(), delivery.getAttempts() + 5);
		NotificationDeliveryRetry audit = new NotificationDeliveryRetry();
		audit.setDeliveryId(id);
		audit.setRequestId(request.requestId().toString());
		audit.setRequestedBy(operator);
		audit.setReason(request.reason().trim());
		audit.setPreviousStatus(delivery.getStatus());
		audit.setPreviousMaxAttempts(delivery.getMaxAttempts());
		audit.setNewMaxAttempts(budget);
		audit.setAttemptsAtRetry(delivery.getAttempts());
		retryRepository.save(audit);
		delivery.setMaxAttempts(budget);
		deliveryService.requeue(delivery);
		return toResponse(delivery);
	}

	private NotificationDelivery find(Long id) {
		return deliveryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", "id", id));
	}

	private boolean retryable(NotificationDelivery delivery) {
		return "FAILED".equals(delivery.getStatus()) || "ABANDONED".equals(delivery.getStatus());
	}

	private NotificationDeliveryResponse toResponse(NotificationDelivery delivery) {
		var notification = delivery.getNotification();
		return new NotificationDeliveryResponse(delivery.getId(), notification == null ? null : notification.getId(),
				delivery.getChannel(), NotificationDeliveryStatus.valueOf(delivery.getStatus()), delivery.getAttempts(),
				delivery.getMaxAttempts(), delivery.getLastError(), delivery.getNextAttemptAt(),
				delivery.getDeliveredAt(), delivery.getCreatedAt(), retryable(delivery),
				notification == null ? null : NotificationContexts.from(notification));
	}

	private Pageable page(Pageable pageable) {
		if (pageable.isUnpaged() || pageable.getPageSize() > 100)
			throw new BusinessException(400, "Page size must be between 1 and 100");
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));
	}
}
