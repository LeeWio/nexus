package space.nebula.nexus.common.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugValidatorTest {

	private final SlugValidator validator = new SlugValidator();

	@Test
	void acceptsSimpleLowercaseSlugs() {
		assertTrue(validator.isValid("linux", null));
		assertTrue(validator.isValid("java", null));
		assertTrue(validator.isValid("next-js", null));
		assertTrue(validator.isValid("spring-boot-3-intro", null));
	}

	@Test
	void rejectsUppercaseLeadingTrailingOrEmptySegments() {
		assertFalse(validator.isValid("Linux", null));
		assertFalse(validator.isValid("linux-", null));
		assertFalse(validator.isValid("-linux", null));
		assertFalse(validator.isValid("linux--distro", null));
		assertFalse(validator.isValid("linux distro", null));
	}

	@Test
	void treatsBlankAsValidSoNotBlankCanOwnPresence() {
		assertTrue(validator.isValid(null, null));
		assertTrue(validator.isValid("", null));
		assertTrue(validator.isValid("   ", null));
	}
}
