package space.nebula.nexus.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a browser-reported path and referrer into the stored analytics fields.
 * Rejects anything that is not a same-site page path.
 */
@Component
public class PageViewNormalizer {

	static final Set<String> SITE_HOSTS = Set.of("easylove.top", "www.easylove.top", "localhost");

	public String normalizePath(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String path = raw.trim();
		int query = path.indexOf('?');
		if (query >= 0) {
			path = path.substring(0, query);
		}
		int hash = path.indexOf('#');
		if (hash >= 0) {
			path = path.substring(0, hash);
		}
		if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\") || path.contains("://")) {
			return null;
		}
		if (path.startsWith("/api/") || path.startsWith("/oauth2/") || path.startsWith("/login/")) {
			return null;
		}
		if (path.length() > 255) {
			path = path.substring(0, 255);
		}
		return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	public String normalizeTitle(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String title = raw.trim().replaceAll("\\s+", " ");
		return title.length() > 160 ? title.substring(0, 160) : title;
	}

	public Referrer classifyReferrer(String raw, String userAgent) {
		if (!StringUtils.hasText(raw) || "null".equalsIgnoreCase(raw)) {
			return wechat(userAgent) ? new Referrer(null, null, "WeChat") : new Referrer(null, null, "Direct");
		}
		String trimmed = raw.trim();
		if (trimmed.length() > 500) {
			trimmed = trimmed.substring(0, 500);
		}
		URI uri;
		try {
			uri = URI.create(trimmed);
		} catch (IllegalArgumentException exception) {
			return new Referrer(trimmed, null, "Referral");
		}
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		if (host.startsWith("www.")) {
			host = host.substring(4);
		}
		String channel = channelFor(host, userAgent);
		return new Referrer(trimmed, host.isBlank() ? null : host, channel);
	}

	private String channelFor(String host, String userAgent) {
		if (host.isBlank()) {
			return wechat(userAgent) ? "WeChat" : "Direct";
		}
		if (SITE_HOSTS.contains(host) || host.endsWith(".easylove.top")) {
			return "Internal";
		}
		if (wechat(userAgent) || host.contains("weixin") || host.contains("wechat") || host.contains("qq.com")) {
			return "WeChat";
		}
		if (host.contains("google.") || host.equals("google.com") || host.contains("baidu.") || host.contains("bing.")
				|| host.contains("duckduckgo.") || host.contains("sogou.") || host.contains("so.com")
				|| host.contains("yahoo.")) {
			return "Search";
		}
		if (host.contains("twitter.") || host.equals("t.co") || host.contains("x.com") || host.contains("facebook.")
				|| host.contains("linkedin.") || host.contains("weibo.") || host.contains("xiaohongshu.")
				|| host.contains("bilibili.")) {
			return "Social";
		}
		return "Referral";
	}

	private boolean wechat(String userAgent) {
		return userAgent != null && userAgent.toLowerCase(Locale.ROOT).contains("micromessenger");
	}

	public record Referrer(String referrer, String host, String channel) {
	}
}
