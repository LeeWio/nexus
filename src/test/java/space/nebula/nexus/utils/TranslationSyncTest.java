package space.nebula.nexus.utils;

import java.util.Map;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.CategoryTranslation;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostTranslation;
import space.nebula.nexus.payload.request.CategoryTranslationRequest;
import space.nebula.nexus.payload.request.PostTranslationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationSyncTest {
	@Test
	void postUpdatesExistingTranslationWithoutReplacingTheRow() {
		Post post = new Post();
		PostTranslation existing = translation(post, "en", "Old title");
		existing.setId(24L);
		post.getTranslations().put("en", existing);

		TranslationSync.post(post, Map.of("en", request("New title", "Updated body")));

		PostTranslation updated = post.getTranslations().get("en");
		assertSame(existing, updated);
		assertEquals(24L, updated.getId());
		assertEquals("New title", updated.getTitle());
		assertEquals("Updated body", updated.getContent());
	}

	@Test
	void postDropsBackfilledSourceLocaleAndRemovesMissingLocales() {
		Post post = new Post();
		post.getTranslations().put("zh", translation(post, "zh", "中文标题"));
		post.getTranslations().put("en", translation(post, "en", "English title"));
		post.getTranslations().put("ja", translation(post, "ja", "日本語"));

		TranslationSync.post(post, Map.of("zh", request("另一份中文", "正文"), "en", request("English title", "Body")));

		assertEquals(1, post.getTranslations().size());
		assertTrue(post.getTranslations().containsKey("en"));
		assertEquals("Body", post.getTranslations().get("en").getContent());
	}

	@Test
	void categoryUpdatesTheExistingLocaleRow() {
		Category category = new Category();
		CategoryTranslation existing = new CategoryTranslation();
		existing.setId(7L);
		existing.setCategory(category);
		existing.setLocale("en");
		existing.setName("Old");
		category.getTranslations().put("en", existing);

		TranslationSync.category(category, Map.of("en", new CategoryTranslationRequest("Notes", "Writing")));

		assertSame(existing, category.getTranslations().get("en"));
		assertEquals("Notes", existing.getName());
		assertEquals("Writing", existing.getDescription());
	}

	private static PostTranslation translation(Post post, String locale, String title) {
		PostTranslation translation = new PostTranslation();
		translation.setPost(post);
		translation.setLocale(locale);
		translation.setTitle(title);
		return translation;
	}

	private static PostTranslationRequest request(String title, String content) {
		return new PostTranslationRequest(title, null, content, null, null);
	}
}
