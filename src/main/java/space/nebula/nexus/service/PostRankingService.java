package space.nebula.nexus.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import space.nebula.nexus.config.BlogDiscoveryProperties;
import space.nebula.nexus.entity.Post;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized scoring service for public blog discovery and recommendation
 * surfaces.
 */
@Service
@RequiredArgsConstructor
public class PostRankingService {
	private final BlogDiscoveryProperties discoveryProperties;
	private final Clock clock = Clock.systemUTC();

	public double discoveryScore(Post post) {
		if (post == null) {
			return 0;
		}

		double score = Boolean.TRUE.equals(post.getIsFeatured())
				? positive(discoveryProperties.getFeaturedWeight(), 1_000)
				: 0;
		score += Math.log1p(nullToZero(post.getViews())) * positive(discoveryProperties.getViewWeight(), 18);
		score += Math.log1p(nullToZero(post.getLikesCount())) * positive(discoveryProperties.getLikeWeight(), 4);
		score += Math.log1p(nullToZero(post.getFavoritesCount()))
				* positive(discoveryProperties.getFavoriteWeight(), 6);
		score += freshnessBoost(post);
		score += contentCompletenessBoost(post);
		return score;
	}

	public double relatedScore(Post source, Post candidate) {
		if (source == null || candidate == null || source == candidate) {
			return 0;
		}

		// Discovery popularity is only a tie-breaker here; semantic relevance stays dominant.
		double score = discoveryScore(candidate) * 0.2;
		if (sameCategory(source, candidate)) {
			score += 120;
		}
		if (sameSeries(source, candidate)) {
			score += 160;
		}
		score += sharedTagCount(source, candidate) * 80;
		if (sameContentType(source, candidate)) {
			score += 20;
		}
		return score;
	}

	/**
	 * Ranks recent momentum separately from lifetime popularity. This keeps the
	 * trending section useful for newer posts that have not accumulated views yet.
	 */
	public double trendingScore(Post post) {
		if (post == null) {
			return 0;
		}

		double score = Math.log1p(nullToZero(post.getViews()))
				* positive(discoveryProperties.getViewWeight(), 18);
		score += Math.log1p(nullToZero(post.getLikesCount()))
				* positive(discoveryProperties.getLikeWeight(), 4) * 1.5;
		score += Math.log1p(nullToZero(post.getFavoritesCount()))
				* positive(discoveryProperties.getFavoriteWeight(), 6) * 1.5;
		return score + freshnessBoost(post) * 1.5 + contentCompletenessBoost(post) * 0.5;
	}

	private double freshnessBoost(Post post) {
		if (post.getPublishedAt() == null) {
			return 0;
		}
		long ageDays = Math.max(0, Duration.between(post.getPublishedAt(), LocalDateTime.now(clock)).toDays());
		int windowDays = positive(discoveryProperties.getFreshnessWindowDays(), 90);
		int halfLifeDays = positive(discoveryProperties.getFreshnessHalfLifeDays(), 30);
		if (ageDays >= windowDays) {
			return 0;
		}

		// Smoothly rewards freshness without letting a single new post dominate for 90 days.
		double decay = Math.exp(-Math.log(2) * ageDays / halfLifeDays);
		return windowDays * positive(discoveryProperties.getFreshnessWeight(), 0.8) * decay;
	}

	private double contentCompletenessBoost(Post post) {
		double score = 0;
		if (StrUtil.isNotBlank(post.getSummary()) || StrUtil.isNotBlank(post.getAutoSummary())) {
			score += positive(discoveryProperties.getSummaryWeight(), 8);
		}
		if (StrUtil.isNotBlank(post.getCoverImage())) {
			score += positive(discoveryProperties.getCoverWeight(), 6);
		}
		if (post.getCategory() != null) {
			score += positive(discoveryProperties.getCategoryWeight(), 4);
		}
		if (post.getWordCount() != null && post.getWordCount() > 0) {
			double contentQuality = Math.min(1.0, post.getWordCount() / 800.0);
			score += contentQuality * positive(discoveryProperties.getContentWeight(), 8);
		}
		if (post.getTags() != null && !post.getTags().isEmpty()) {
			score += Math.min(post.getTags().size(), 5) * positive(discoveryProperties.getTagWeight(), 2);
		}
		if (post.getSeries() != null) {
			score += positive(discoveryProperties.getSeriesWeight(), 3);
		}
		return score;
	}

	private boolean sameCategory(Post source, Post candidate) {
		return source.getCategory() != null && candidate.getCategory() != null && source.getCategory().getId() != null
				&& source.getCategory().getId().equals(candidate.getCategory().getId());
	}

	private boolean sameSeries(Post source, Post candidate) {
		return source.getSeries() != null && candidate.getSeries() != null && source.getSeries().getId() != null
				&& source.getSeries().getId().equals(candidate.getSeries().getId());
	}

	private boolean sameContentType(Post source, Post candidate) {
		return source.getContentType() != null && source.getContentType().equals(candidate.getContentType());
	}

	private long sharedTagCount(Post source, Post candidate) {
		if (source.getTags() == null || candidate.getTags() == null || source.getTags().isEmpty()
				|| candidate.getTags().isEmpty()) {
			return 0;
		}
		Set<Long> sourceTagIds = source.getTags().stream().filter(tag -> tag.getId() != null)
				.map(space.nebula.nexus.entity.Tag::getId).collect(Collectors.toSet());
		return candidate.getTags().stream().filter(tag -> tag.getId() != null)
				.filter(tag -> sourceTagIds.contains(tag.getId())).count();
	}

	private long nullToZero(Long value) {
		return value == null ? 0 : Math.max(0, value);
	}

	private int positive(int configured, int fallback) {
		return configured > 0 ? configured : fallback;
	}

	private double positive(double configured, double fallback) {
		return configured > 0 ? configured : fallback;
	}
}
