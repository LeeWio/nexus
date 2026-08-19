package space.nebula.nexus.utils;

import cn.hutool.core.lang.Assert;
import org.springframework.util.StringUtils;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes user-created social topics into stable, URL-safe-enough slugs
 * without tying them to the article Tag vocabulary.
 */
public final class MomentTopicPolicy {

	public static final int MAX_TOPICS_PER_MOMENT = 3;
	public static final int MAX_SLUG_CODE_POINTS = 80;

	private MomentTopicPolicy() {
	}

	public static List<String> normalizeTopicSlugs(List<String> topicSlugs) {
		if (topicSlugs == null) {
			return List.of();
		}

		Assert.isTrue(topicSlugs.size() <= MAX_TOPICS_PER_MOMENT, () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"A moment can contain at most " + MAX_TOPICS_PER_MOMENT + " topics"));

		List<String> normalizedSlugs = new ArrayList<>(topicSlugs.size());
		Set<String> uniqueSlugs = new LinkedHashSet<>();
		for (String topicSlug : topicSlugs) {
			String normalized = normalizeSlug(topicSlug);
			Assert.isTrue(uniqueSlugs.add(normalized),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment topics must be unique"));
			normalizedSlugs.add(normalized);
		}
		return List.copyOf(normalizedSlugs);
	}

	public static String normalizeSlug(String value) {
		Assert.isTrue(StringUtils.hasText(value),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment topic cannot be blank"));

		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceFirst("^#+", "")
				.toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "-").replaceAll("[^\\p{L}\\p{N}\\p{M}-]+", "-")
				.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
		Assert.isTrue(StringUtils.hasText(normalized),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Moment topic must contain letters or numbers"));
		Assert.isTrue(normalized.codePointCount(0, normalized.length()) <= MAX_SLUG_CODE_POINTS,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Moment topic must not exceed " + MAX_SLUG_CODE_POINTS + " characters"));
		return normalized;
	}
}
