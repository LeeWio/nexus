package space.nebula.nexus.common.constant;

/**
 * Constants for Redis cache keys and prefixes. Follows the naming convention:
 * nexus:{module}:{sub_module}:{identifier}
 */
public final class CacheConstants {
	private CacheConstants() {
	}

	// --- Global Config ---
	public static final String CACHE_PREFIX = "nexus:cache:";
	public static final String DEFAULT_DELIMITER = "::";

	// --- Domain Names (Used in @Cacheable value) ---
	public static final String BLOG_POSTS = "blog_posts";
	public static final String CATEGORIES = "categories";
	public static final String TAGS = "tags";
	public static final String USERS = "users";
	public static final String ANALYTICS = "analytics";
	public static final String SYS_CONFIG = "sys_config";
	public static final String SITE_STATS = "site_stats";
	public static final String FRIEND_LINKS = "friendLinks";
	public static final String NAVIGATION = "navigation";
	public static final String MOMENTS = "moments";
	public static final String PROJECTS = "projects";
	public static final String SEO = "seo";
	public static final String MARKET_INDICES = "marketIndices";
	public static final String GITHUB_STATS = "github_stats";

	// --- Key Patterns & Prefixes ---

	// Blog related
	public static final String POST_LIST_KEY = "'list:' + #categoryId + ':' + #tagId + ':' + #keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort";
	public static final String BLOG_DISCOVERY_KEY = "'discovery'";
	public static final String POST_SLUG_KEY = "'slug:' + #slug";
	public static final String POST_SLUG_PREFIX = "nexus:post:slug:";
	public static final String POST_AUTOSAVE_PREFIX = "nexus:post:autosave:";
	public static final String POST_VIEW_EXTRA_HASH = "nexus:post:views:extra";

	// Interaction
	public static final String POST_LIKES_SET = "post:likes:set:";
	public static final String POST_FAVORITES_SET = "post:favorites:set:";

	// Security & Auth
	public static final String LOGIN_FAIL_COUNT = "nexus:security:login_fail:";
	public static final String LOGIN_LOCK = "nexus:security:login_lock:";
	public static final String RATE_LIMIT_PREFIX = "nexus:rate_limit:";
	public static final String OTP_CODE = "nexus:security:otp:";

	// SEO
	public static final String SITEMAP_KEY = "sitemap";
	public static final String RSS_FEED_KEY = "rss_feed";
	public static final String SITEMAP_SPEL = "'sitemap'";
	public static final String RSS_FEED_SPEL = "'rss_feed'";

	// Analytics
	public static final String ANALYTICS_BUFFER_KEY = "nexus:analytics:buffer";
	public static final String OPERATION_LOG_BUFFER_KEY = "nexus:operation_log:buffer";
	public static final String OVERVIEW_KEY = "'overview'";

	// Dashboard
	public static final String PUBLIC_DASHBOARD_KEY = "'public_dashboard'";

	// Config
	public static final String PUBLIC_CONFIGS_KEY = "'public_configs'";

	// FriendLink & Menu & Project
	public static final String PUBLIC_LIST_KEY = "'public_list'";
	public static final String PUBLIC_TREE_KEY = "'public_tree'";

	// Market
	public static final String MARKET_1D = "1D";

	// GitHub
	public static final String GITHUB_STATS_CACHE_KEY = "nexus:github:global_stats";

	// Locks
	public static final String LOCK_KANBAN_COLUMN_PREFIX = "nexus:lock:kanban:column:";

	/**
	 * Build a full cache key compatible with Spring Cache's Redis naming
	 * convention. Use this when manually deleting or accessing Spring-managed cache
	 * entries via RedisUtil.
	 */
	public static String buildFullKey(String domain, String key) {
		return CACHE_PREFIX + domain + DEFAULT_DELIMITER + key;
	}
}
