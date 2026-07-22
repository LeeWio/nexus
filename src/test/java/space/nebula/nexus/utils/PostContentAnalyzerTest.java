package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostContentAnalyzerTest
{
	@Test
	void analyzeExtractsReadingMetadataAndToc()
	{
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
	void analyzeUsesManualSummaryAsAutoSummarySource()
	{
		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze("Title", " Manual **summary** ",
				"Long body");

		assertEquals("Manual summary", metadata.autoSummary());
	}
}
