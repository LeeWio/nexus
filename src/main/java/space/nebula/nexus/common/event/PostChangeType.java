package space.nebula.nexus.common.event;

/** Business reason that caused a post to change. */
public enum PostChangeType {
	CREATED,
	UPDATED,
	SUBMITTED_FOR_REVIEW,
	PUBLISHED,
	REJECTED
}
