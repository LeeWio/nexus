package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordServiceImplTest {

	@InjectMocks
	private SensitiveWordServiceImpl sensitiveWordService;

	@Test
	void testSensitiveWordDetectionAndFilter() {
		sensitiveWordService.init();

		// 1. Test standard match (exact)
		assertTrue(sensitiveWordService.containsSensitiveWord("badword1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is badword1"));

		// 2. Test Case-Insensitive Bypass
		assertTrue(sensitiveWordService.containsSensitiveWord("BaDwOrD1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is BaDwOrD1"));

		// 3. Test Punctuation/Noise Bypass
		assertTrue(sensitiveWordService.containsSensitiveWord("b*a*d*w*o*r*d*1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is b*a*d*w*o*r*d*1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is b_a_d_w_o_r_d_1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is b.a.d.w.o.r.d.1"));

		// 4. Test Spacing Bypass
		assertTrue(sensitiveWordService.containsSensitiveWord("b a d w o r d 1"));
		assertEquals("This is ***", sensitiveWordService.filter("This is b a d w o r d 1"));
	}
}
