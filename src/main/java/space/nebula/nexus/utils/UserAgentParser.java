package space.nebula.nexus.utils;

import cn.hutool.core.util.StrUtil;

/**
 * Parses a browser User-Agent into the fields the analytics dashboard shows.
 * Classification is deliberately local: a missed agent becomes Unknown, never a
 * guessed brand.
 */
public final class UserAgentParser {

	public record ClientProfile(String browser, String osName, String deviceType, String clientName, boolean bot) {
	}

	private UserAgentParser() {
	}

	public static ClientProfile parse(String userAgent) {
		String raw = StrUtil.nullToEmpty(userAgent).trim();
		String agent = raw.toLowerCase();
		if (raw.isEmpty() || isBot(agent, raw)) {
			return new ClientProfile(browserOf(agent), osOf(agent), ClientDeviceClassifier.classify(raw),
					clientOf(agent, raw), true);
		}
		return new ClientProfile(browserOf(agent), osOf(agent), ClientDeviceClassifier.classify(raw),
				clientOf(agent, raw), false);
	}

	static boolean isBot(String agent, String raw) {
		if (raw.equals("node") || raw.startsWith("Bun/") || raw.startsWith("curl/") || raw.startsWith("Wget/")) {
			return true;
		}
		return agent.contains("bot") || agent.contains("spider") || agent.contains("crawler")
				|| agent.contains("headless") || agent.contains("rootevidence") || agent.contains("ahrefs")
				|| agent.contains("semrush") || agent.contains("mj12bot") || agent.contains("dotbot")
				|| agent.contains("petalbot") || agent.contains("bytespider") || agent.contains("gptbot")
				|| agent.contains("claudebot") || agent.contains("amazonbot") || agent.contains("mshors");
	}

	private static String clientOf(String agent, String raw) {
		if (agent.contains("micromessenger")) {
			return "微信";
		}
		if (isBot(agent, raw)) {
			if (agent.contains("ahrefs"))
				return "AhrefsBot";
			if (agent.contains("rootevidence"))
				return "RootEvidence";
			if (agent.contains("headless"))
				return "Headless Chrome";
			if (raw.equals("node"))
				return "Node";
			if (raw.startsWith("Bun/"))
				return "Bun";
			return "Bot";
		}
		return browserOf(agent);
	}

	private static String browserOf(String agent) {
		if (agent.contains("edg/") || agent.contains("edge/"))
			return "Edge";
		if (agent.contains("firefox/") || agent.contains("fxios"))
			return "Firefox";
		if (agent.contains("micromessenger"))
			return "微信";
		if (agent.contains("crios/") || agent.contains("chrome/") || agent.contains("chromium"))
			return "Chrome";
		if (agent.contains("safari/") && !agent.contains("chrome") && !agent.contains("chromium"))
			return "Safari";
		if (agent.isBlank())
			return "Unknown";
		return "Unknown";
	}

	private static String osOf(String agent) {
		if (agent.contains("iphone") || agent.contains("ipad") || agent.contains("ios"))
			return agent.contains("ipad") ? "iPadOS" : "iOS";
		if (agent.contains("android"))
			return "Android";
		if (agent.contains("cros"))
			return "ChromeOS";
		if (agent.contains("mac os") || agent.contains("macintosh"))
			return "macOS";
		if (agent.contains("windows"))
			return "Windows";
		if (agent.contains("linux") || agent.contains("x11"))
			return "Linux";
		return "Unknown";
	}
}
