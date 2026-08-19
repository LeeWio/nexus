package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MomentContentPolicyTest {

	@Test
	void countsVisibleTextInsteadOfRichTextJsonMarkup() {
		String content = """
				{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Field note"}]}]}
				""";

		assertEquals(10, MomentContentPolicy.visibleCharacterCount(content));
		assertTrue(MomentContentPolicy.hasVisibleText(content));
	}

	@Test
	void recognizesAnEmptyRichTextDocumentAsEmpty() {
		String content = "{" + "\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";

		assertEquals(0, MomentContentPolicy.visibleCharacterCount(content));
		assertFalse(MomentContentPolicy.hasVisibleText(content));
	}

	@Test
	void preservesTheComposerLimitForPlainText() {
		assertEquals(MomentContentPolicy.MAX_VISIBLE_CHARACTERS,
				MomentContentPolicy.visibleCharacterCount("a".repeat(MomentContentPolicy.MAX_VISIBLE_CHARACTERS)));
		assertEquals(MomentContentPolicy.MAX_VISIBLE_CHARACTERS + 1,
				MomentContentPolicy.visibleCharacterCount("a".repeat(MomentContentPolicy.MAX_VISIBLE_CHARACTERS + 1)));
	}
}
