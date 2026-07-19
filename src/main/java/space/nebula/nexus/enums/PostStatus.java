package space.nebula.nexus.enums;

import lombok.Getter;

/**
 * Represents the possible statuses of a blog post or article in the content
 * management workflow.
 */
@Getter
public enum PostStatus
{
	/**
	 * The post is saved as a draft and not yet submitted for review.
	 */
	DRAFT("Draft"),

	/**
	 * The post has been submitted and is awaiting editorial review.
	 */
	PENDING_REVIEW("Pending Review"),

	/**
	 * The post has passed editorial review and will be published at a specified
	 * future time.
	 */
	SCHEDULED("Scheduled"),

	/**
	 * The post has been approved and is publicly visible.
	 */
	PUBLISHED("Published"),

	/**
	 * The post was reviewed and rejected, typically due to policy or quality
	 * issues.
	 */
	REJECTED("Rejected"),

	/**
	 * The post was once published but is now archived and no longer actively
	 * displayed.
	 */
	ARCHIVED("Archived");

	private final String description;

	PostStatus(String description)
	{
		this.description = description;
	}
}
