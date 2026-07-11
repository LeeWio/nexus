package space.nebula.nexus.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import space.nebula.nexus.common.exception.BusinessException;

class OutboundUrlValidatorTest {

	private final OutboundUrlValidator validator = new OutboundUrlValidator();

	@Test
	void rejectsLoopbackAndPrivateAddresses() {
		assertThrows(BusinessException.class, () -> validator.validate("http://127.0.0.1/admin"));
		assertThrows(BusinessException.class, () -> validator.validate("http://10.0.0.1/internal"));
		assertThrows(BusinessException.class, () -> validator.validate("http://169.254.169.254/latest/meta-data"));
	}

	@Test
	void rejectsUnsupportedOrAmbiguousUrls() {
		assertThrows(BusinessException.class, () -> validator.validate("file:///etc/passwd"));
		assertThrows(BusinessException.class, () -> validator.validate("http://user@example.com/path"));
		assertThrows(BusinessException.class, () -> validator.validate("not-a-url"));
	}
}
