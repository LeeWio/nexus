package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.enums.PostContentType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostContentAnalyzerTest {
	@Test
	void analyzeExtractsReadingMetadataAndTocFromMarkdown() {
		String content = """
				# Intro
				这里是一段中文内容 about Spring Boot.

				## Details
				More content with **markdown** and a link https://example.com.
				""";

		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", null, content);

		assertTrue(metadata.wordCount() > 10);
		assertEquals(1, metadata.readingTimeMinutes());
		assertTrue(metadata.autoSummary().contains("中文内容"));
		assertTrue(metadata.toc().contains("\"text\":\"Intro\""));
		assertTrue(metadata.toc().contains("\"anchor\":\"intro\""));
		assertFalse(metadata.contentHash().isBlank());
	}

	@Test
	void analyzeUsesManualSummaryAsAutoSummarySource() {
		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", " Manual **summary** ",
				"Long body");

		assertEquals("Manual summary", metadata.autoSummary());
	}

	@Test
	void analyzeExtractsVisibleTextAndHeadingsFromTiptapJson() {
		String jsonContent = """
				{
				  "type": "doc",
				  "content": [
				    {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"标题"}]},
				    {"type":"paragraph","content":[{"type":"text","text":"Hello world"}]},
				    {"type":"image","attrs":{"src":"https://example.test/image.png"}},
				    {"type":"paragraph","content":[{"type":"text","text":"正文"}]}
				  ]
				}
				""";

		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", null, jsonContent,
				PostContentType.JSON);

		assertEquals(6, metadata.wordCount());
		assertEquals("标题 Hello world 正文", metadata.autoSummary());
		assertTrue(metadata.toc().contains("\"level\":2"));
		assertTrue(metadata.toc().contains("\"text\":\"标题\""));
		assertTrue(metadata.toc().contains("\"anchor\":\"标题\""));
	}

	@Test
	void analyzeStillReadsLegacyEditorJsJsonWhileUsingNoMetadataAsText() {
		String jsonContent = """
				{"blocks":[{"data":{"text":"Hello world from JSON block editor."}},{"data":{"text":"Second block."}}]}
				""";

		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", null, jsonContent,
				PostContentType.JSON);

		assertEquals("Hello world from JSON block editor. Second block.", metadata.autoSummary());
		assertEquals("[]", metadata.toc());
	}
}
