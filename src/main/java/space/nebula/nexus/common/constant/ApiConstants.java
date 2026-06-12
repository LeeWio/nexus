package space.nebula.nexus.common.constant;

/**
 * Constants for API versioning and global mapping.
 */
public final class ApiConstants {
    private ApiConstants() {}

    public static final String V1 = "/api/v1";
    
    // Domain specific paths
    public static final String ADMIN = V1 + "/admin";
    public static final String PUBLIC = V1 + "/public";
    public static final String AUTH = V1 + "/auth";
}
