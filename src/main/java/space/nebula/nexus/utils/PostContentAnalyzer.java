package space.nebula.nexus.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import space.nebula.nexus.enums.PostContentType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts deterministic metadata from post content for discovery, SEO, and
 * reading UX.
 *
 * <p>
 * JSON posts use the persisted Tiptap document as the source of truth. Only
 * text nodes contribute to visible text, while heading nodes contribute to the
 * table of contents. Legacy Editor.js blocks remain readable for migration
 * tooling, but new content must use the Tiptap document shape.
 * </p>
 */
public final class PostContentAnalyzer {
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final int SUMMARY_LIMIT = 160;
	private static final int TOC_LIMIT = 30;
	private static final int WORDS_PER_MINUTE = 300;
	private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
	private static final Pattern MARKDOWN_FENCE = Pattern.compile("^\\s*```.*$");
	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Pattern MARKDOWN_DECORATION = Pattern.compile("[`*_~>#\\-!\\[\\]()]+");
	private static final Pattern LATIN_WORD = Pattern.compile("[\\p{IsAlphabetic}\\p{IsDigit}]+");

	private PostContentAnalyzer() {
	}

	public static Metadata analyze(String title, String summary, String content) {
		return analyze(title, summary, content, PostContentType.MDX);
	}

	public static Metadata analyze(String title, String summary, String content, PostContentType contentType) {
		String normalizedContent = StrUtil.blankToDefault(content, "");
		Document document = contentType == PostContentType.JSON
				? readJsonDocument(normalizedContent)
				: new Document(toPlainText(normalizedContent), markdownHeadings(normalizedContent));
		String plainText = document.text();
		int wordCount = countWords(plainText);
		int readingTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / (double) WORDS_PER_MINUTE));
		String autoSummary = StrUtil.blankToDefault(cleanSummary(summary), createSummary(plainText));
		String toc = writeToc(document.headings());
		String contentHash = SecureUtil.sha256(normalizeForHash(title) + "\n" + normalizeForHash(normalizedContent));
		return new Metadata(wordCount, readingTimeMinutes, autoSummary, toc, contentHash);
	}

	/** Extract visible text from legacy Markdown/HTML content. */
	public static String toPlainText(String content) {
		return toPlainText(content, PostContentType.MDX);
	}

	/** Extract visible text using the declared post representation. */
	public static String toPlainText(String content, PostContentType contentType) {
		if (contentType == PostContentType.JSON) {
			return readJsonDocument(StrUtil.blankToDefault(content, "")).text();
		}
		if (StrUtil.isBlank(content)) {
			return "";
		}
		String text = HTML_TAG.matcher(content).replaceAll(" ");
		text = MARKDOWN_DECORATION.matcher(text).replaceAll(" ");
		text = text.replaceAll("https?://\\S+", " ");
		return normalizeWhitespace(text);
	}

	private static Document readJsonDocument(String content) {
		if (StrUtil.isBlank(content)) {
			return new Document("", List.of());
		}
		try {
			JsonNode root = JSON.readTree(content);
			StringBuilder text = new StringBuilder();
			List<Heading> headings = new ArrayList<>();
			if ("doc".equals(root.path("type").asText()) && root.path("content").isArray()) {
				appendRichText(root, text, headings);
			} else if (root.path("blocks").isArray()) {
				for (JsonNode block : root.path("blocks")) {
					text.append(toPlainText(block.path("data").path("text").asText(""))).append(' ');
				}
			}
			return new Document(normalizeWhitespace(text.toString()), headings);
		} catch (JsonProcessingException ignored) {
			return new Document("", List.of());
		}
	}

	private static void appendRichText(JsonNode node, StringBuilder text, List<Heading> headings) {
		String type = node.path("type").asText();
		switch (type) {
			case "text" -> text.append(node.path("text").asText(""));
			case "hardBreak" -> text.append(' ');
			case "mention" -> {
				String label = node.path("attrs").path("label").asText("");
				if (!label.isBlank()) {
					text.append('@').append(label);
				}
			}
			default -> {
				int start = text.length();
				for (JsonNode child : node.path("content")) {
					appendRichText(child, text, headings);
				}
				if ("heading".equals(type) && headings.size() < TOC_LIMIT) {
					String label = normalizeWhitespace(text.substring(start));
					if (!label.isEmpty()) {
						int level = Math.max(1, Math.min(6, node.path("attrs").path("level").asInt(1)));
						String anchor = node.path("attrs").path("data-toc-id").asText("");
						headings.add(new Heading(level, label, anchor));
					}
				}
				if (text.length() > start) {
					text.append(' ');
				}
			}
		}
	}

	private static String normalizeWhitespace(String text) {
		return text.replaceAll("(?U)\\s+", " ").strip();
	}

	private static int countWords(String plainText) {
		if (StrUtil.isBlank(plainText)) {
			return 0;
		}
		int cjkCharacters = 0;
		StringBuilder latinBuffer = new StringBuilder();
		for (int i = 0; i < plainText.length();) {
			int codePoint = plainText.codePointAt(i);
			i += Character.charCount(codePoint);
			Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
			if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
					|| script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL) {
				cjkCharacters++;
				latinBuffer.append(' ');
			} else {
				latinBuffer.appendCodePoint(codePoint);
			}
		}
		int latinWords = 0;
		var matcher = LATIN_WORD.matcher(latinBuffer);
		while (matcher.find()) {
			latinWords++;
		}
		return cjkCharacters + latinWords;
	}

	private static String createSummary(String plainText) {
		return StrUtil.isBlank(plainText) ? null : truncate(plainText, SUMMARY_LIMIT);
	}

	private static String cleanSummary(String summary) {
		return StrUtil.isBlank(summary) ? null : truncate(toPlainText(summary), 500);
	}

	private static String truncate(String text, int maxCharacters) {
		int characters = text.codePointCount(0, text.length());
		return text.substring(0, text.offsetByCodePoints(0, Math.min(characters, maxCharacters))).strip();
	}

	private static List<Heading> markdownHeadings(String content) {
		List<Heading> headings = new ArrayList<>();
		boolean inFence = false;
		for (String line : content.split("\\R")) {
			if (MARKDOWN_FENCE.matcher(line).matches()) {
				inFence = !inFence;
				continue;
			}
			if (inFence) {
				continue;
			}
			var matcher = MARKDOWN_HEADING.matcher(line);
			if (matcher.matches()) {
				String text = toPlainText(matcher.group(2));
				if (!text.isEmpty()) {
					headings.add(new Heading(matcher.group(1).length(), text, ""));
					if (headings.size() == TOC_LIMIT) {
						break;
					}
				}
			}
		}
		return headings;
	}

	private static String writeToc(List<Heading> headings) {
		Set<String> reservedAnchors = new HashSet<>();
		for (Heading heading : headings) {
			if (!heading.anchor().isBlank()) {
				reservedAnchors.add(heading.anchor());
			}
		}
		Set<String> usedAnchors = new HashSet<>();
		List<Heading> toc = new ArrayList<>();
		for (Heading heading : headings) {
			String anchor = heading.anchor();
			if (anchor.isBlank() || !usedAnchors.add(anchor)) {
				String base = toAnchor(heading.text());
				anchor = base;
				for (int suffix = 2; reservedAnchors.contains(anchor) || !usedAnchors.add(anchor); suffix++) {
					anchor = base + "-" + suffix;
				}
			}
			toc.add(new Heading(heading.level(), heading.text(), anchor));
		}
		try {
			return JSON.writeValueAsString(toc);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to serialize article headings", e);
		}
	}

	private static String toAnchor(String text) {
		String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
		String anchor = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+", "-").replaceAll("^-+|-+$",
				"");
		return StrUtil.blankToDefault(anchor, "section");
	}

	private static String normalizeForHash(String value) {
		return value == null ? "" : value.replace("\r\n", "\n").trim();
	}

	private record Document(String text, List<Heading> headings) {
	}

	private record Heading(int level, String text, String anchor) {
	}

	public record Metadata(int wordCount, int readingTimeMinutes, String autoSummary, String toc, String contentHash) {
	}
}
