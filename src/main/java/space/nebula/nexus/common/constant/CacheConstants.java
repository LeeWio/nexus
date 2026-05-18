package space.nebula.nexus.common.constant;

/**
 * Constants for Redis cache keys.
 */
public final class CacheConstants {
    private CacheConstants() {}

    public static final String BLOG_POSTS = "blog_posts";
    public static final String CATEGORIES = "categories";
    public static final String TAGS = "tags";
    public static final String USERS = "users";
    
    // Key patterns
    public static final String POST_LIST_KEY = "'list:' + #categoryId + ':' + #tagId + ':' + #keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize";
    public static final String POST_SLUG_KEY = "'slug:' + #slug";
    public static final String POST_SLUG_PREFIX = "nexus:post:slug:";
    public static final String POST_AUTOSAVE_PREFIX = "nexus:post:autosave:";
    public static final String POST_VIEW_COUNT = "post:view_count:";
    public static final String POST_VIEW_EXTRA_HASH = "nexus:post:views:extra";
    
    // Interaction constants
    public static final String POST_LIKES_SET = "post:likes:set:";
    public static final String POST_FAVORITES_SET = "post:favorites:set:";
    
    // Security constants
    public static final String LOGIN_FAIL_COUNT = "security:login_fail:";
    public static final String LOGIN_LOCK = "security:login_lock:";
    public static final String RATE_LIMIT = "security:rate_limit:";
    public static final String OTP_CODE = "security:otp:";
}
