package space.nebula.nexus.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.MomentChangeType;
import space.nebula.nexus.common.event.MomentChangedEvent;
import space.nebula.nexus.service.IXSyncService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentXSyncListener {

	private final IXSyncService xSyncService;

	@Async("outboundExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onMomentChanged(MomentChangedEvent event) {
		if (event.getChangeType() != MomentChangeType.CREATED || !event.isShareToX()) {
			return;
		}
		try {
			xSyncService.processPending(event.getMomentId());
		} catch (Exception e) {
			log.warn("Moment X sync listener failed for moment {}: {}", event.getMomentId(), e.getMessage());
		}
	}
}
