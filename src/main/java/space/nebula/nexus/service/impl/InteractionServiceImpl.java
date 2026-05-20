package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.utils.RedisUtil;

import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.security.util.SecurityUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements IInteractionService {

	private final RedisUtil redisUtil;
	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Override
	public ApiResponse<Void> likePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		validatePostExists(postId);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		return ApiResponse.success("Post liked", null);
	}

	@Override
	public ApiResponse<Void> unlikePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		String key = CacheConstants.POST_LIKES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		return ApiResponse.success("Post unliked", null);
	}

	@Override
	public ApiResponse<Void> favoritePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		validatePostExists(postId);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setAdd(key, user.getId().toString());
		return ApiResponse.success("Post favorited", null);
	}

	@Override
	public ApiResponse<Void> unfavoritePost(Long postId) {
		User user = SecurityUtil.getCurrentUserOrThrow(userRepository);
		String key = CacheConstants.POST_FAVORITES_SET + postId;
		redisUtil.setRemove(key, user.getId().toString());
		return ApiResponse.success("Post unfavorited", null);
	}

	@Override
	public void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder,
			Long postId) {
		String username = SecurityUtil.getCurrentUsername();
		if (username != null) {
			userRepository.findByUsername(username).ifPresent(user -> {
				String likeKey = CacheConstants.POST_LIKES_SET + postId;
				String favKey = CacheConstants.POST_FAVORITES_SET + postId;

				Boolean isLiked = redisUtil.setIsMember(likeKey, user.getId().toString());
				Boolean isFavorited = redisUtil.setIsMember(favKey, user.getId().toString());

				builder.isLiked(isLiked != null ? isLiked : false);
				builder.isFavorited(isFavorited != null ? isFavorited : false);
			});
		} else {
			builder.isLiked(false);
			builder.isFavorited(false);
		}
	}

	private void validatePostExists(Long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ResourceNotFoundException("Post", "id", postId);
		}
	}
}
