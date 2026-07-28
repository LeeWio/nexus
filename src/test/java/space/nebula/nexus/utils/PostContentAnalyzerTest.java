package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.enums.PostContentType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostContentAnalyzerTest {
	@Test
	void analyzeExtractsReadingMetadataAndToc() {
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
	void analyzeExtractsCleanTextFromJsonBlocks() {
		String jsonContent = """
				{
				  "time": 1550476186479,
				  "blocks": [
				    {
				      "id": "abc",
				      "type": "paragraph",
				      "data": {
				        "text": "Hello world from JSON block editor."
				      }
				    },
				    {
				      "id": "def",
				      "type": "paragraph",
				      "data": {
				        "text": "This is the second block."
				      }
				    }
				  ],
				  "version": "2.8.1"
				}
				""";

		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", null, jsonContent, PostContentType.JSON);

		// Clean words count should be: "Hello", "world", "from", "JSON", "block", "editor", "This", "is", "the", "second", "block" (11 words)
		// It should NOT include blocks, time, type, data, paragraph, id, version etc.
		assertEquals(11, metadata.wordCount());
		assertEquals("Hello world from JSON block editor. This is the second block.", metadata.autoSummary());
	}
}
