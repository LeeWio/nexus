package space.nebula.nexus.enums;

/**
 * Enumeration of user account statuses.
 */
public enum UserStatus {
    /** The user account is active and can be used normally. */
    ACTIVE,

    /** The user account is inactive and cannot be used. */
    INACTIVE,

    /** The user account is pending and waiting for administrator approval. */
    PENDING,

    /** The user account is banned due to violations or other reasons. */
    BANNED,

    /** The user account is deleted and can no longer be used. */
    DELETED;
}
