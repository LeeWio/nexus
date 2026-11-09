package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.service.PageViewNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAgentParserTest {

	private final PageViewNormalizer normalizer = new PageViewNormalizer();

	@Test
	void parsesWechatIphoneSafariAndBots() {
		var wechat = UserAgentParser.parse(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 MicroMessenger/8.0.80");
		assertEquals("微信", wechat.clientName());
		assertEquals("iOS", wechat.osName());
		assertEquals("Mobile", wechat.deviceType());
		assertFalse(wechat.bot());

		var safari = UserAgentParser.parse(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 Version/18.7 Mobile/15E148 Safari/604.1");
		assertEquals("Safari", safari.browser());
		assertEquals("iOS", safari.osName());

		var chrome = UserAgentParser.parse(
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.0.0 Safari/537.36");
		assertEquals("Chrome", chrome.browser());
		assertEquals("Linux", chrome.osName());
		assertEquals("Desktop", chrome.deviceType());

		assertTrue(UserAgentParser.parse("Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)").bot());
		assertTrue(UserAgentParser.parse("node").bot());
	}

	@Test
	void normalizesPathsAndReferrers() {
		assertEquals("/single/me-and-machines", normalizer.normalizePath("/single/me-and-machines?utm=1"));
		assertEquals("/", normalizer.normalizePath("/"));
		assertNull(normalizer.normalizePath("/api/v1/public/moments"));
		assertNull(normalizer.normalizePath("https://evil.example/phish"));
		assertNull(normalizer.normalizePath("//easylove.top"));

		assertEquals("Direct", normalizer.classifyReferrer(null, "Safari").channel());
		assertEquals("WeChat", normalizer.classifyReferrer("", "MicroMessenger/8.0").channel());
		assertEquals("Internal", normalizer.classifyReferrer("https://easylove.top/moments", "Chrome").channel());
		assertEquals("Search",
				normalizer.classifyReferrer("https://www.google.com/search?q=odyssey", "Chrome").channel());
		assertEquals("Referral", normalizer.classifyReferrer("https://news.ycombinator.com/item", "Firefox").channel());
	}
}
