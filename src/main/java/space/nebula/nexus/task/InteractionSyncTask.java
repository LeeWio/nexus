package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionSyncTask {

    private final RedisUtil redisUtil;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 120000) // Run every 2 minutes
    @Transactional
    public void syncInteractionsToDb() {
        log.info("Starting interaction sync task from Redis to MySQL...");
        
        List<Post> posts = postRepository.findAll();
        for (Post post : posts) {
            String likeKey = CacheConstants.POST_LIKES_SET + post.getId();
            String favKey = CacheConstants.POST_FAVORITES_SET + post.getId();
            
            Long likesSize = redisUtil.setSize(likeKey);
            Long favsSize = redisUtil.setSize(favKey);
            
            boolean updated = false;
            
            if (likesSize != null && !likesSize.equals(post.getLikesCount())) {
                post.setLikesCount(likesSize);
                updated = true;
            }
            
            if (favsSize != null && !favsSize.equals(post.getFavoritesCount())) {
                post.setFavoritesCount(favsSize);
                updated = true;
            }
            
            if (updated) {
                postRepository.save(post);
                log.debug("Updated interaction counts for post {}", post.getId());
            }
        }
        
        log.info("Finished interaction sync task.");
    }
}
