package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDeviceClassifierTest {

	@Test
	void classifiesPhonesAsMobile() {
		assertEquals(ClientDeviceClassifier.MOBILE, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"));
		assertEquals(ClientDeviceClassifier.MOBILE, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"));
	}

	@Test
	void classifiesTabletsBeforePhones() {
		assertEquals(ClientDeviceClassifier.TABLET, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (iPad; CPU OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"));
		assertEquals(ClientDeviceClassifier.TABLET, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (Linux; Android 13; SM-X900) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"));
		assertEquals(ClientDeviceClassifier.TABLET, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15 Mobile/15E148"));
	}

	@Test
	void classifiesDesktopAndMissingAgents() {
		assertEquals(ClientDeviceClassifier.DESKTOP, ClientDeviceClassifier.classify(
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"));
		assertEquals(ClientDeviceClassifier.DESKTOP, ClientDeviceClassifier.classify(null));
		assertEquals(ClientDeviceClassifier.DESKTOP, ClientDeviceClassifier.classify(""));
	}
}
