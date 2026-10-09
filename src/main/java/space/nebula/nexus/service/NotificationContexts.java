package space.nebula.nexus.service;

import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.payload.response.NotificationContext;

/**
 * Maps persisted context without reconstructing historical data from text or
 * links.
 */
public final class NotificationContexts {
	private NotificationContexts() {
	}
	public static NotificationContext from(Notification notification) {
		if (notification.getObjectType() == null || notification.getObjectId() == null
				|| notification.getAction() == null)
			return null;
		return new NotificationContext(NotificationContext.ObjectType.valueOf(notification.getObjectType()),
				notification.getObjectId(), notification.getActorId(),
				NotificationContext.Action.valueOf(notification.getAction()));
	}
}
