package space.nebula.nexus.enums;

public enum FriendLinkStatus
{
	/**
	 * The friend link request has been submitted and is awaiting review.
	 */
	APPLYING,

	/**
	 * The friend link request has been reviewed and approved; the link is active.
	 */
	APPROVED,

	/**
	 * The friend link request has been reviewed and rejected; the link will not be
	 * displayed.
	 */
	REJECTED
}