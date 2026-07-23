package space.nebula.nexus.common.event;

/** Business reason that caused a post to change. */
public enum PostChangeType {
	CREATED, UPDATED, SUBMITTED_FOR_REVIEW, WITHDRAWN_FROM_REVIEW, SCHEDULED, SCHEDULE_CANCELED, PUBLISHED, REJECTED, ARCHIVED, RESTORED_TO_DRAFT
}
