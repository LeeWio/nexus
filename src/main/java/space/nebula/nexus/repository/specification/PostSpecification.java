package space.nebula.nexus.repository.specification;

import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable specifications for Post entity queries.
 */
public class PostSpecification {

	/**
	 * Builds a specification for public post searching with multiple criteria.
	 */
	public static Specification<Post> filterPublicPosts(Long categoryId, Long tagId, String keyword) {
		return filterPublicPosts(categoryId, tagId, keyword, null, null, null);
	}

	public static Specification<Post> filterPublicPosts(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType) {
		return filterPosts(PostStatus.PUBLISHED, categoryId, tagId, keyword, featuredOnly, hasCover, contentType);
	}

	/**
	 * Generic filter for posts with status, category, tag and keyword.
	 */
	public static Specification<Post> filterPosts(PostStatus status, Long categoryId, Long tagId, String keyword) {
		return filterPosts(status, categoryId, tagId, keyword, null, null, null);
	}

	public static Specification<Post> filterPosts(PostStatus status, Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("id"), categoryId));
			}

			if (tagId != null) {
				Join<Post, Tag> tagsJoin = root.join("tags");
				predicates.add(cb.equal(tagsJoin.get("id"), tagId));
			}

			if (StrUtil.isNotBlank(keyword)) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern),
						cb.like(cb.lower(root.get("summary")), pattern),
						cb.like(cb.lower(root.get("content")), pattern)));
			}

			if (Boolean.TRUE.equals(featuredOnly)) {
				predicates.add(cb.isTrue(root.get("isFeatured")));
			}

			if (Boolean.TRUE.equals(hasCover)) {
				predicates.add(cb.isNotNull(root.get("coverImage")));
				predicates.add(cb.notEqual(root.get("coverImage"), ""));
			}

			if (contentType != null) {
				predicates.add(cb.equal(root.get("contentType"), contentType));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
