package space.nebula.nexus.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.CommentRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads approved root-comment totals onto posts without Hibernate {@code @Formula}.
 * Formula fields break paginated/joined SQL on H2 and some dialect subquery rewrites.
 */
@Component
@RequiredArgsConstructor
public class PostCommentCountSupport {

	private final CommentRepository commentRepository;

	public void attachApprovedRootCounts(Collection<Post> posts) {
		if (posts == null || posts.isEmpty()) {
			return;
		}
		List<Long> postIds = posts.stream().map(Post::getId).filter(Objects::nonNull).distinct().toList();
		if (postIds.isEmpty()) {
			return;
		}

		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : commentRepository.countRootCommentsByPostIds(postIds, CommentStatus.APPROVED)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		for (Post post : posts) {
			if (post.getId() == null) {
				continue;
			}
			post.setCommentsCount(counts.getOrDefault(post.getId(), 0L));
		}
	}
}
