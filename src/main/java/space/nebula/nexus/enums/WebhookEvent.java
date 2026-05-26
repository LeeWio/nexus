package space.nebula.nexus.enums;

import lombok.Getter;

/**
 * Supported webhook events that can be triggered by system actions. These
 * events allow external services to react to changes within the application,
 * such as post updates or new comment submissions.
 */
@Getter
public enum WebhookEvent
{
	/**
	 * Triggered when a blog post is published for the first time or updated after
	 * publication. This event may include the full post payload in the webhook
	 * request.
	 */
	POST_PUBLISHED("Triggered when a post is published or updated"),

	/**
	 * Triggered when a blog post is permanently deleted from the system. The
	 * webhook payload typically includes the ID and metadata of the deleted post.
	 */
	POST_DELETED("Triggered when a post is deleted"),

	/**
	 * Triggered when a user submits a new comment on a post. The comment may be in
	 * a pending state (e.g., awaiting moderation), and the payload usually contains
	 * comment details and associated post information.
	 */
	COMMENT_SUBMITTED("Triggered when a new comment is submitted");

	private final String description;

	WebhookEvent(String description)
	{
		this.description = description;
	}
}