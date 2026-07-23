package space.nebula.nexus.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import space.nebula.nexus.config.BlogDiscoveryProperties;
import space.nebula.nexus.entity.Post;

import java.time.Duration;
import java.time.LocalDateTime;
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

	public double discoveryScore(Post post) {
		double score = Boolean.TRUE.equals(post.getIsFeatured())
				? positive(discoveryProperties.getFeaturedWeight(), 1_000)
				: 0;
		score += Math.log1p(nullToZero(post.getViews())) * positive(discoveryProperties.getViewWeight(), 18);
		score += nullToZero(post.getLikesCount()) * positive(discoveryProperties.getLikeWeight(), 4);
		score += nullToZero(post.getFavoritesCount()) * positive(discoveryProperties.getFavoriteWeight(), 6);
		score += freshnessBoost(post);
		score += contentCompletenessBoost(post);
		return score;
	}

	public double relatedScore(Post source, Post candidate) {
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

	private double freshnessBoost(Post post) {
		if (post.getPublishedAt() == null) {
			return 0;
		}
		long ageDays = Math.max(0, Duration.between(post.getPublishedAt(), LocalDateTime.now()).toDays());
		return Math.max(0, positive(discoveryProperties.getFreshnessWindowDays(), 90) - ageDays)
				* positive(discoveryProperties.getFreshnessWeight(), 0.8);
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
		return value == null ? 0 : value;
	}

	private int positive(int configured, int fallback) {
		return configured > 0 ? configured : fallback;
	}

	private double positive(double configured, double fallback) {
		return configured > 0 ? configured : fallback;
	}
}
