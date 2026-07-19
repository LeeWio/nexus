package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.utils.RedisUtil;

import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.security.util.SecurityUtil;
import cn.hutool.core.lang.Assert;
import org.springframework.cache.CacheManager;
import space.nebula.nexus.entity.Post;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements IInteractionService
{

	private final RedisUtil redisUtil;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;
	private final CacheManager cacheManager;

	@Override
	@Transactional
	public ApiResponse<Void> likePost(Long postId)
	{
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePublishedPost(postId);
		int inserted = jdbcTemplate.update("INSERT IGNORE INTO blog_post_like(post_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				postId, user.getId());
		if (inserted > 0) postRepository.incrementLikes(postId, 1L);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post liked", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> unlikePost(Long postId)
	{
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePostExists(postId);
		int deleted = jdbcTemplate.update("DELETE FROM blog_post_like WHERE post_id = ? AND user_id = ?", postId, user.getId());
		if (deleted > 0) postRepository.incrementLikes(postId, -1L);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post unliked", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> favoritePost(Long postId)
	{
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePublishedPost(postId);
		int inserted = jdbcTemplate.update("INSERT IGNORE INTO blog_post_favorite(post_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				postId, user.getId());
		if (inserted > 0) postRepository.incrementFavorites(postId, 1L);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post favorited", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> unfavoritePost(Long postId)
	{
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Post post = validatePostExists(postId);
		int deleted = jdbcTemplate.update("DELETE FROM blog_post_favorite WHERE post_id = ? AND user_id = ?", postId, user.getId());
		if (deleted > 0) postRepository.incrementFavorites(postId, -1L);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		evictCaches(post);
		return ApiResponse.success("Post unfavorited", null);
	}

	@Override
	public void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder,
			Long postId)
	{
		String username = SecurityUtil.getCurrentUsername();
		if (username != null)
		{
			userRepository.findByUsername(username).ifPresent(user ->
			{
				String likeKey = CacheConstants.POST_LIKES_SET + postId;
				String favKey = CacheConstants.POST_FAVORITES_SET + postId;

				Boolean isLiked = jdbcTemplate.queryForObject(
						"SELECT EXISTS(SELECT 1 FROM blog_post_like WHERE post_id = ? AND user_id = ?)",
						Boolean.class, postId, user.getId());
				Boolean isFavorited = jdbcTemplate.queryForObject(
						"SELECT EXISTS(SELECT 1 FROM blog_post_favorite WHERE post_id = ? AND user_id = ?)",
						Boolean.class, postId, user.getId());

				builder.isLiked(isLiked != null ? isLiked : false);
				builder.isFavorited(isFavorited != null ? isFavorited : false);
			});
		}
		else
		{
			builder.isLiked(false);
			builder.isFavorited(false);
		}
	}

	private void evictCaches(Post post)
	{
		// 1. Evict manual post slug cache
		String slugKey = CacheConstants.POST_SLUG_PREFIX + post.getSlug();
		redisUtil.delete(slugKey);

		// 2. Evict Spring-managed list/discovery caches for blog posts
		org.springframework.cache.Cache cache = cacheManager.getCache(CacheConstants.BLOG_POSTS);
		if (cache != null)
		{
			cache.clear();
		}
	}

	private Post validatePostExists(Long postId)
	{
		return postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
	}

	private Post validatePublishedPost(Long postId)
	{
		var post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(post.isPublished(), () -> new space.nebula.nexus.common.exception.BusinessException(
				space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST, "Only published posts can be interacted with"));
		return post;
	}
}
