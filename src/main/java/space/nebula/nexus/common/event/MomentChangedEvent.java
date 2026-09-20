package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.enums.MomentVisibility;

/**
 * Event published when a moment is created, updated, or deleted.
 */
@Getter
public class MomentChangedEvent extends ApplicationEvent {

	private final Long momentId;
	private final MomentChangeType changeType;
	private final MomentVisibility visibility;
	private final boolean shareToX;

	public MomentChangedEvent(Object source, Long momentId, MomentChangeType changeType, MomentVisibility visibility,
			boolean shareToX) {
		super(source);
		this.momentId = momentId;
		this.changeType = changeType;
		this.visibility = visibility;
		this.shareToX = shareToX;
	}
}
