package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a system configuration is updated or deleted.
 */
@Getter
public class ConfigChangedEvent extends ApplicationEvent {
	private final String configKey;
	private final boolean deleted;

	public ConfigChangedEvent(Object source, String configKey, boolean deleted) {
		super(source);
		this.configKey = configKey;
		this.deleted = deleted;
	}
}
