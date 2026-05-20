package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SlugUtilTest {

	@Test
	public void testToSlug() {
		assertEquals("hello-world", SlugUtil.toSlug("Hello World"));
		assertEquals("java-21-is-awesome", SlugUtil.toSlug("Java 21 is Awesome!"));
		assertEquals("my-first-post", SlugUtil.toSlug("My First Post..."));
		assertEquals("trim-test", SlugUtil.toSlug("  Trim Test  "));
		assertEquals("", SlugUtil.toSlug(null));
		assertEquals("", SlugUtil.toSlug("   "));
		assertEquals("special-chars-123", SlugUtil.toSlug("Special chars @#$%^&*() 123"));
	}
}
