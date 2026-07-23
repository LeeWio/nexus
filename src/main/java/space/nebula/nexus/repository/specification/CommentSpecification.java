package space.nebula.nexus.repository.specification;

import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for Comment entity queries.
 */
public class CommentSpecification {

	public static Specification<Comment> filterComments(CommentStatus status, Long postId, String username,
			String keyword) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			if (postId != null) {
				predicates.add(cb.equal(root.get("post").get("id"), postId));
			}

			if (StrUtil.isNotBlank(username)) {
				predicates.add(cb.equal(root.get("user").get("username"), username));
			}

			if (StrUtil.isNotBlank(keyword)) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				predicates.add(cb.like(cb.lower(root.get("content")), pattern));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
