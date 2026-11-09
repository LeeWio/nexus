package space.nebula.nexus.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.AnalyticsProperties;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.TrafficClass;
import space.nebula.nexus.payload.request.PageViewRequest;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.utils.IpUtil;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.UserAgentParser;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Accepts a browser page view, classifies it, and buffers it. The request
 * thread does not write MySQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageViewCollector {

	private final AnalyticsPrivacyService privacyService;
	private final AnalyticsProperties properties;
	private final PageViewNormalizer normalizer;
	private final RedisUtil redisUtil;

	public boolean collect(PageViewRequest request, String sessionId, HttpServletRequest servletRequest) {
		String path = normalizer.normalizePath(request.path());
		if (path == null || sessionId == null || sessionId.isBlank()) {
			return false;
		}
		String userAgent = AnalyticsPrivacyService.clientUserAgent(servletRequest);
		String ip = IpUtil.getIpAddress(servletRequest);
		String visitorHash = privacyService.hashVisitor(ip, userAgent);
		String dedupeKey = CacheConstants.PAGE_VIEW_DEDUPE_PREFIX + sessionId + ":" + path;
		boolean firstInWindow = redisUtil.lock(dedupeKey, "1", properties.getPageViewDedupeMinutes(), TimeUnit.MINUTES);
		if (!firstInWindow) {
			return false;
		}

		UserAgentParser.ClientProfile profile = UserAgentParser.parse(userAgent);
		PageViewNormalizer.Referrer referrer = normalizer.classifyReferrer(request.referrer(), userAgent);
		User user = SecurityUtil.getCurrentUser();

		PageView view = new PageView();
		view.setViewedAt(LocalDateTime.now());
		view.setPath(path);
		view.setTitle(normalizer.normalizeTitle(request.title()));
		view.setSessionId(sessionId);
		view.setVisitorHash(visitorHash);
		view.setIpAddress(truncate(ip, 45));
		view.setBrowser(profile.browser());
		view.setOsName(profile.osName());
		view.setDeviceType(profile.deviceType());
		view.setClientName(profile.clientName());
		view.setReferrer(referrer.referrer());
		view.setReferrerHost(referrer.host());
		view.setSourceChannel(referrer.channel());
		view.setUserAgent(truncate(userAgent, 500));
		view.setUserId(user == null ? null : user.getId());
		view.setTrafficClass(classify(profile, visitorHash, user));
		view.setIsDeleted(false);

		Long pushed = redisUtil.listAdd(CacheConstants.PAGE_VIEW_BUFFER_KEY, view);
		if (pushed == null) {
			redisUtil.delete(dedupeKey);
			log.warn("Page view buffer rejected path {}", path);
			return false;
		}
		return true;
	}

	private TrafficClass classify(UserAgentParser.ClientProfile profile, String visitorHash, User user) {
		if (profile.bot()) {
			return TrafficClass.BOT;
		}
		if (SecurityUtil.hasRole("ADMIN")) {
			return TrafficClass.OWNER;
		}
		if (user != null && user.getRoles() != null && user.getRoles().stream().anyMatch(
				role -> role.getCode() != null && role.getCode().toUpperCase(Locale.ROOT).contains("ADMIN"))) {
			return TrafficClass.OWNER;
		}
		if (properties.getOwnerHashes() != null && properties.getOwnerHashes().contains(visitorHash)) {
			return TrafficClass.OWNER;
		}
		return TrafficClass.HUMAN;
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
