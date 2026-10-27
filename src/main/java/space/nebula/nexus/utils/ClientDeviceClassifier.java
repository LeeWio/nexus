package space.nebula.nexus.utils;

import cn.hutool.core.util.StrUtil;

/**
 * Classifies a User-Agent into the dashboard device buckets.
 *
 * <p>Tablet is checked before phone. Android tablets often omit "Mobile", and
 * iPadOS 13+ reports a Macintosh user agent with a touch hint.
 */
public final class ClientDeviceClassifier {

	public static final String MOBILE = "Mobile";
	public static final String DESKTOP = "Desktop";
	public static final String TABLET = "Tablet";

	private ClientDeviceClassifier() {
	}

	/**
	 * Returns {@code Mobile}, {@code Tablet}, or {@code Desktop} for the agent.
	 *
	 * @param userAgent raw User-Agent header, possibly null
	 * @return one of the device bucket names
	 */
	public static String classify(String userAgent) {
		String agent = StrUtil.nullToEmpty(userAgent).toLowerCase();
		if (agent.isBlank() || "null".equals(agent)) {
			return DESKTOP;
		}
		if (isTablet(agent)) {
			return TABLET;
		}
		if (isMobile(agent)) {
			return MOBILE;
		}
		return DESKTOP;
	}

	private static boolean isTablet(String agent) {
		return agent.contains("ipad") || agent.contains("tablet") || agent.contains("kindle")
				|| agent.contains("silk/") || agent.contains("playbook") || agent.contains("kftt")
				|| agent.contains("kfjwi") || agent.contains("kfjwa") || agent.contains("kfapwi")
				|| agent.contains("kfthwi") || agent.contains("kfsowi") || agent.contains("kfapwa")
				|| (agent.contains("android") && !agent.contains("mobile"))
				|| (agent.contains("macintosh") && agent.contains("mobile"));
	}

	private static boolean isMobile(String agent) {
		return agent.contains("iphone") || agent.contains("ipod") || agent.contains("android")
				|| agent.contains("mobile") || agent.contains("windows phone") || agent.contains("blackberry")
				|| agent.contains("opera mini") || agent.contains("iemobile");
	}
}
