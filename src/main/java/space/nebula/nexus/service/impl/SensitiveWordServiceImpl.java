package space.nebula.nexus.service.impl;

import cn.hutool.dfa.WordTree;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import space.nebula.nexus.service.SensitiveWordService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of SensitiveWordService using Hutool DFA WordTree.
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

	private final WordTree wordTree = new WordTree();

	@PostConstruct
	public void init() {
		log.info("Initializing sensitive word dictionary...");
		try {
			ClassPathResource resource = new ClassPathResource("dict/sensitive_words.txt");
			if (resource.exists()) {
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
					reader.lines().filter(line -> !line.isBlank()).map(String::trim).map(this::normalizeText)
							.forEach(wordTree::addWord);
				}
			}
			log.info("Sensitive word dictionary initialized successfully.");
		} catch (Exception e) {
			log.error("Failed to load sensitive words dictionary", e);
		}
	}

	@Override
	public boolean containsSensitiveWord(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String normalized = normalizeText(text);
		return wordTree.isMatch(normalized);
	}

	@Override
	public String filter(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}

		String normalized = normalizeText(text);
		java.util.List<String> matchedWords = wordTree.matchAll(normalized, -1, true, true);

		if (matchedWords.isEmpty()) {
			return text;
		}

		String result = text;
		for (String match : matchedWords) {
			String fuzzyRegex = buildFuzzyRegex(match);
			result = result.replaceAll(fuzzyRegex, "***");
		}

		return result;
	}

	private String normalizeText(String text) {
		if (text == null) {
			return "";
		}
		// Convert to lowercase and strip all punctuation, spacing, and symbols
		// (currency, math, modifier)
		return text.toLowerCase().replaceAll("[\\s\\p{Punct}\\p{Sc}\\p{Sm}\\p{So}\\p{Sk}]+", "");
	}

	private String buildFuzzyRegex(String word) {
		StringBuilder regex = new StringBuilder("(?i)");
		String noisePattern = "[\\s\\p{Punct}\\p{Sc}\\p{Sm}\\p{So}\\p{Sk}]*";

		for (int i = 0; i < word.length(); i++) {
			if (i > 0) {
				regex.append(noisePattern);
			}
			regex.append(java.util.regex.Pattern.quote(String.valueOf(word.charAt(i))));
		}
		return regex.toString();
	}
}
