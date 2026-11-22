package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.MomentEmailDeliveryService;
import space.nebula.nexus.service.MomentEmailDeliveryService.Attempt;
import space.nebula.nexus.utils.MailUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentEmailDeliveryTask {
	private static final int BATCH_SIZE = 20;

	private final MomentEmailDeliveryService deliveries;
	private final MailUtil mailUtil;

	@Scheduled(fixedDelay = 10_000)
	@SchedulerLock(name = "momentEmailDelivery", lockAtMostFor = "PT10M")
	public void sendDue() {
		for (Attempt attempt : deliveries.claimDue(BATCH_SIZE)) {
			boolean sent;
			try {
				var mail = deliveries.prepare(attempt);
				mail.ifPresent(message -> mailUtil.sendTemplateMail(message.recipient(), message.subject(),
						"moment-subscription", message.variables(), message.headers()));
				sent = mail.isPresent();
			} catch (Exception error) {
				deliveries.fail(attempt, error);
				log.warn("Moment email delivery {} attempt {} failed: {}", attempt.id(), attempt.number(),
						error.getClass().getSimpleName());
				continue;
			}
			deliveries.complete(attempt, sent);
		}
	}
}
