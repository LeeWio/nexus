package space.nebula.nexus.enums;

/**
 * Status for blog comments.
 */
public enum CommentStatus {
	/**
	 * The comment is awaiting moderation and has not yet been reviewed.
	 */
	PENDING,

	/**
	 * The comment has been reviewed and approved for public display.
	 */
	APPROVED,

	/**
	 * The comment has been reviewed and rejected; it will not be displayed.
	 */
	REJECTED,

	/**
	 * The comment is identified as spam and automatically or manually marked as
	 * such.
	 */
	SPAM
}