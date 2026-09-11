package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserAvatarsTest {

	@Test
	void prefersStoredAvatarOverEmailAndGithub() {
		assertEquals("https://cdn.example/me.png", UserAvatars.resolve("https://cdn.example/me.png",
				"just.vireo@gmail.com", "LeeWio"));
	}

	@Test
	void usesGithubAvatarWhenNoStoredAvatar() {
		assertEquals("https://github.com/LeeWio.png",
				UserAvatars.resolve(null, "just.vireo@gmail.com", "LeeWio"));
	}

	@Test
	void usesGravatarWhenOnlyEmailIsPresent() {
		assertEquals(
				"https://www.gravatar.com/avatar/c02e5605cf09189cd25faf458db2be5f1a72d1d41e54553a838c17a11224c591?s=256&d=identicon",
				UserAvatars.resolve("  ", "just.vireo@gmail.com", null));
	}

	@Test
	void returnsNullWhenNoAvatarSourceExists() {
		assertNull(UserAvatars.resolve(null, null, null));
		assertNull(UserAvatars.resolve(" ", "not-an-email", "not a login"));
	}
}
