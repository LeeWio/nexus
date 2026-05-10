package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.utils.RedisUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements IInteractionService {

    private final RedisUtil redisUtil;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public ApiResponse<Void> likePost(Long postId) {
        Long userId = getCurrentUserId();
        validatePostExists(postId);
        String key = CacheConstants.POST_LIKES_SET + postId;
        redisUtil.setAdd(key, userId.toString());
        return ApiResponse.success("Post liked", null);
    }

    @Override
    public ApiResponse<Void> unlikePost(Long postId) {
        Long userId = getCurrentUserId();
        String key = CacheConstants.POST_LIKES_SET + postId;
        redisUtil.setRemove(key, userId.toString());
        return ApiResponse.success("Post unliked", null);
    }

    @Override
    public ApiResponse<Void> favoritePost(Long postId) {
        Long userId = getCurrentUserId();
        validatePostExists(postId);
        String key = CacheConstants.POST_FAVORITES_SET + postId;
        redisUtil.setAdd(key, userId.toString());
        // Note: For absolute consistency, user_favorite_post table should also be updated.
        // We'll let the scheduled task handle the DB persistence.
        return ApiResponse.success("Post favorited", null);
    }

    @Override
    public ApiResponse<Void> unfavoritePost(Long postId) {
        Long userId = getCurrentUserId();
        String key = CacheConstants.POST_FAVORITES_SET + postId;
        redisUtil.setRemove(key, userId.toString());
        return ApiResponse.success("Post unfavorited", null);
    }

    @Override
    public void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder, Long postId) {
        try {
            Long userId = getCurrentUserId();
            String likeKey = CacheConstants.POST_LIKES_SET + postId;
            String favKey = CacheConstants.POST_FAVORITES_SET + postId;
            
            Boolean isLiked = redisUtil.setIsMember(likeKey, userId.toString());
            Boolean isFavorited = redisUtil.setIsMember(favKey, userId.toString());
            
            builder.isLiked(isLiked != null ? isLiked : false);
            builder.isFavorited(isFavorited != null ? isFavorited : false);
        } catch (BusinessException e) {
            // User not logged in, they can't like/favorite
            builder.isLiked(false);
            builder.isFavorited(false);
        }
        
        // Dynamic counts can also be aggregated here from Redis + DB if needed,
        // but for now, we rely on the DB counts updated by the Sync Task.
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(401, "Please log in to perform this action");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException(401, "User not found"));
    }

    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(404, "Post not found");
        }
    }
}
