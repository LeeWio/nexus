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
public class SensitiveWordServiceImpl implements SensitiveWordService
{

	private final WordTree wordTree = new WordTree();

	@PostConstruct
	public void init()
	{
		log.info("Initializing sensitive word dictionary...");
		try
		{
			ClassPathResource resource = new ClassPathResource("dict/sensitive_words.txt");
			if (resource.exists())
			{
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)))
				{
					reader.lines().filter(line -> !line.isBlank()).forEach(wordTree::addWord);
				}
			}
			log.info("Sensitive word dictionary initialized successfully.");
		}
		catch (Exception e)
		{
			log.error("Failed to load sensitive words dictionary", e);
		}
	}

	@Override
	public boolean containsSensitiveWord(String text)
	{
		if (text == null || text.isBlank())
		{
			return false;
		}
		return wordTree.isMatch(text);
	}

	@Override
	public String filter(String text)
	{
		if (text == null || text.isBlank())
		{
			return text;
		}
		// Hutool's WordTree doesn't have a direct "replace" in older versions,
		// but it can find all matches. For simplicity in this demo:
		return wordTree.matchAll(text, -1, true, true).stream().reduce(text, (res, match) -> res.replace(match, "***"),
				(a, b) -> a);
	}
}
