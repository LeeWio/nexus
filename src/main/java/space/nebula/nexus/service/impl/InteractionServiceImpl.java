package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.CommentInteractionResponse;
import space.nebula.nexus.payload.response.PostInteractionResponse;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.utils.RedisUtil;

import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.security.util.SecurityUtil;
import cn.hutool.core.lang.Assert;
import org.springframework.cache.CacheManager;
import space.nebula.nexus.entity.Post;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements IInteractionService {

	private final RedisUtil redisUtil;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;
	private final CacheManager cacheManager;

	@Override
	@Transactional
	public ApiResponse<PostInteractionResponse> likePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePublishedPost(postId);
		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_post_like(post_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				postId, user.getId());
		if (inserted > 0)
			postRepository.incrementLikes(postId, 1L);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post liked", getPostInteractionResponse(postId, user.getId()));
	}

	@Override
	@Transactional
	public ApiResponse<PostInteractionResponse> unlikePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePostExists(postId);
		int deleted = jdbcTemplate.update("DELETE FROM blog_post_like WHERE post_id = ? AND user_id = ?", postId,
				user.getId());
		if (deleted > 0)
			postRepository.incrementLikes(postId, -1L);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post unliked", getPostInteractionResponse(postId, user.getId()));
	}

	@Override
	@Transactional
	public ApiResponse<CommentInteractionResponse> likeComment(Long commentId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		validateApprovedComment(commentId);
		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_comment_like(comment_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				commentId, user.getId());
		if (inserted > 0) {
			commentRepository.incrementLikes(commentId, 1L);
		}
		return ApiResponse.success("Comment liked", getCommentInteractionResponse(commentId, user.getId()));
	}

	@Override
	@Transactional
	public ApiResponse<CommentInteractionResponse> unlikeComment(Long commentId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		validateCommentExists(commentId);
		int deleted = jdbcTemplate.update("DELETE FROM blog_comment_like WHERE comment_id = ? AND user_id = ?",
				commentId, user.getId());
		if (deleted > 0) {
			commentRepository.incrementLikes(commentId, -1L);
		}
		return ApiResponse.success("Comment unliked", getCommentInteractionResponse(commentId, user.getId()));
	}

	@Override
	@Transactional
	public ApiResponse<PostInteractionResponse> favoritePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePublishedPost(postId);
		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_post_favorite(post_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				postId, user.getId());
		if (inserted > 0)
			postRepository.incrementFavorites(postId, 1L);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post favorited", getPostInteractionResponse(postId, user.getId()));
	}

	@Override
	@Transactional
	public ApiResponse<PostInteractionResponse> unfavoritePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePostExists(postId);
		int deleted = jdbcTemplate.update("DELETE FROM blog_post_favorite WHERE post_id = ? AND user_id = ?", postId,
				user.getId());
		if (deleted > 0)
			postRepository.incrementFavorites(postId, -1L);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post unfavorited", getPostInteractionResponse(postId, user.getId()));
	}

	/**
	 * Reads the response from the database after bulk counter updates. The Post
	 * instance loaded for validation can retain pre-update values in the JPA
	 * persistence context, so it must not be used to build this response.
	 */
	private PostInteractionResponse getPostInteractionResponse(Long postId, Long userId) {
		Map<String, Object> row = jdbcTemplate.queryForMap("""
				SELECT p.likes_count AS likesCount,
				       p.favorites_count AS favoritesCount,
				       EXISTS(SELECT 1 FROM blog_post_like l WHERE l.post_id = p.id AND l.user_id = ?) AS liked,
				       EXISTS(SELECT 1 FROM blog_post_favorite f WHERE f.post_id = p.id AND f.user_id = ?) AS favorited
				FROM blog_post p
				WHERE p.id = ?
				""", userId, userId, postId);
		return new PostInteractionResponse(postId, booleanValue(row.get("liked")), booleanValue(row.get("favorited")),
				longValue(row.get("likesCount")), longValue(row.get("favoritesCount")));
	}

	/**
	 * Reads the response from the database after a bulk comment counter update
	 * rather than relying on the validation entity's potentially stale state.
	 */
	private CommentInteractionResponse getCommentInteractionResponse(Long commentId, Long userId) {
		Map<String, Object> row = jdbcTemplate.queryForMap("""
				SELECT c.likes_count AS likesCount,
				       EXISTS(SELECT 1 FROM blog_comment_like l WHERE l.comment_id = c.id AND l.user_id = ?) AS liked
				FROM blog_comment c
				WHERE c.id = ?
				""", userId, commentId);
		return new CommentInteractionResponse(commentId, booleanValue(row.get("liked")),
				longValue(row.get("likesCount")));
	}

	private boolean booleanValue(Object value) {
		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}
		if (value instanceof Number number) {
			return number.intValue() != 0;
		}
		return Boolean.parseBoolean(String.valueOf(value));
	}

	private long longValue(Object value) {
		return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
	}

	@Override
	public void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder,
			Long postId) {
		User user = SecurityUtil.getCurrentUser();
		if (user == null) {
			String username = SecurityUtil.getCurrentUsername();
			if (username != null) {
				user = userRepository.findByUsername(username).orElse(null);
			}
		}

		if (user != null) {
			final Long userId = user.getId();
			String likeKey = CacheConstants.POST_LIKES_SET + postId;
			String favKey = CacheConstants.POST_FAVORITES_SET + postId;

			Boolean isLiked = jdbcTemplate.queryForObject(
					"SELECT EXISTS(SELECT 1 FROM blog_post_like WHERE post_id = ? AND user_id = ?)", Boolean.class,
					postId, userId);
			Boolean isFavorited = jdbcTemplate.queryForObject(
					"SELECT EXISTS(SELECT 1 FROM blog_post_favorite WHERE post_id = ? AND user_id = ?)", Boolean.class,
					postId, userId);

			builder.isLiked(isLiked != null ? isLiked : false);
			builder.isFavorited(isFavorited != null ? isFavorited : false);
		} else {
			builder.isLiked(false);
			builder.isFavorited(false);
		}
	}

	private void evictCaches(Post post) {
		// 1. Evict manual post slug cache
		String slugKey = CacheConstants.POST_SLUG_PREFIX + post.getSlug();
		redisUtil.delete(slugKey);

		// 2. Evict Spring-managed list/discovery caches for blog posts
		org.springframework.cache.Cache cache = cacheManager.getCache(CacheConstants.BLOG_POSTS);
		if (cache != null) {
			cache.clear();
		}
		org.springframework.cache.Cache discoveryCache = cacheManager.getCache(CacheConstants.BLOG_DISCOVERY);
		if (discoveryCache != null) {
			discoveryCache.clear();
		}
		org.springframework.cache.Cache relatedCache = cacheManager.getCache(CacheConstants.BLOG_RELATED);
		if (relatedCache != null) {
			relatedCache.clear();
		}
	}

	private Post validatePostExists(Long postId) {
		return postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
	}

	private Post validatePublishedPost(Long postId) {
		var post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(post.isPublished(),
				() -> new space.nebula.nexus.common.exception.BusinessException(
						space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST,
						"Only published posts can be interacted with"));
		return post;
	}

	private space.nebula.nexus.entity.Comment validateCommentExists(Long commentId) {
		return commentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
	}

	private space.nebula.nexus.entity.Comment validateApprovedComment(Long commentId) {
		var comment = validateCommentExists(commentId);
		Assert.isTrue(comment.getStatus() == space.nebula.nexus.enums.CommentStatus.APPROVED,
				() -> new space.nebula.nexus.common.exception.BusinessException(
						space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST,
						"Only approved comments can be interacted with"));
		Assert.isFalse(comment.isDeletedPlaceholder(),
				() -> new space.nebula.nexus.common.exception.BusinessException(
						space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST,
						"Deleted comments cannot be interacted with"));
		return comment;
	}
}
