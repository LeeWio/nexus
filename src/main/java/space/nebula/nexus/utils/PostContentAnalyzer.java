package space.nebula.nexus.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extracts deterministic metadata from post content for discovery, SEO, and
 * reading UX.
 */
public final class PostContentAnalyzer
{
	private static final int SUMMARY_LIMIT = 160;
	private static final int TOC_LIMIT = 30;
	private static final int WORDS_PER_MINUTE = 300;
	private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
	private static final Pattern MARKDOWN_FENCE = Pattern.compile("^\\s*```.*$");
	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Pattern JSON_DECORATION = Pattern.compile("[{}\\[\\]\",:]+");
	private static final Pattern MARKDOWN_DECORATION = Pattern.compile("[`*_~>#\\-!\\[\\]()]+");
	private static final Pattern LATIN_WORD = Pattern.compile("[\\p{IsAlphabetic}\\p{IsDigit}]+");

	private PostContentAnalyzer()
	{
	}

	public static Metadata analyze(String title, String summary, String content)
	{
		String normalizedContent = StrUtil.blankToDefault(content, "");
		String plainText = toPlainText(normalizedContent);
		int wordCount = countWords(plainText);
		int readingTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / (double) WORDS_PER_MINUTE));
		String autoSummary = StrUtil.blankToDefault(cleanSummary(summary), createSummary(plainText));
		String toc = buildToc(normalizedContent);
		String contentHash = SecureUtil.sha256(normalizeForHash(title) + "\n" + normalizeForHash(normalizedContent));
		return new Metadata(wordCount, readingTimeMinutes, autoSummary, toc, contentHash);
	}

	public static String toPlainText(String content)
	{
		if (StrUtil.isBlank(content))
		{
			return "";
		}
		String text = HTML_TAG.matcher(content).replaceAll(" ");
		text = JSON_DECORATION.matcher(text).replaceAll(" ");
		text = MARKDOWN_DECORATION.matcher(text).replaceAll(" ");
		text = text.replaceAll("https?://\\S+", " ");
		return text.replaceAll("\\s+", " ").trim();
	}

	private static int countWords(String plainText)
	{
		if (StrUtil.isBlank(plainText))
		{
			return 0;
		}
		int cjkCharacters = 0;
		StringBuilder latinBuffer = new StringBuilder();
		for (int i = 0; i < plainText.length(); i++)
		{
			char ch = plainText.charAt(i);
			Character.UnicodeScript script = Character.UnicodeScript.of(ch);
			if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
					|| script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL)
			{
				cjkCharacters++;
				latinBuffer.append(' ');
			}
			else
			{
				latinBuffer.append(ch);
			}
		}
		int latinWords = 0;
		var matcher = LATIN_WORD.matcher(latinBuffer);
		while (matcher.find())
		{
			latinWords++;
		}
		return cjkCharacters + latinWords;
	}

	private static String createSummary(String plainText)
	{
		if (StrUtil.isBlank(plainText))
		{
			return null;
		}
		if (plainText.length() <= SUMMARY_LIMIT)
		{
			return plainText;
		}
		return plainText.substring(0, SUMMARY_LIMIT).trim();
	}

	private static String cleanSummary(String summary)
	{
		if (StrUtil.isBlank(summary))
		{
			return null;
		}
		String cleaned = toPlainText(summary);
		return cleaned.length() <= 500 ? cleaned : cleaned.substring(0, 500).trim();
	}

	private static String buildToc(String content)
	{
		if (StrUtil.isBlank(content))
		{
			return "[]";
		}
		List<String> items = new ArrayList<>();
		boolean inFence = false;
		for (String line : content.split("\\R"))
		{
			if (MARKDOWN_FENCE.matcher(line).matches())
			{
				inFence = !inFence;
				continue;
			}
			if (inFence)
			{
				continue;
			}
			var matcher = MARKDOWN_HEADING.matcher(line);
			if (matcher.matches())
			{
				int level = matcher.group(1).length();
				String text = toPlainText(matcher.group(2));
				if (StrUtil.isBlank(text))
				{
					continue;
				}
				items.add("{\"level\":" + level + ",\"text\":\"" + escapeJson(text) + "\",\"anchor\":\""
						+ escapeJson(toAnchor(text)) + "\"}");
				if (items.size() >= TOC_LIMIT)
				{
					break;
				}
			}
		}
		return "[" + String.join(",", items) + "]";
	}

	private static String toAnchor(String text)
	{
		String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFKD);
		String anchor = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+", "-")
				.replaceAll("^-+|-+$", "");
		return StrUtil.blankToDefault(anchor, "section");
	}

	private static String normalizeForHash(String value)
	{
		return value == null ? "" : value.replace("\r\n", "\n").trim();
	}

	private static String escapeJson(String value)
	{
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	public record Metadata(int wordCount, int readingTimeMinutes, String autoSummary, String toc, String contentHash)
	{
	}
}
