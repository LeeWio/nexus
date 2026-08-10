package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;

import java.util.Set;

/**
 * Immutable post state captured by a revision.
 */
public record PostRevisionSnapshot(String title, String slug, String coverImage, String summary, String content,
		PostContentType contentType, PostStatus status, Boolean featured, Long categoryId, Set<Long> tagIds,
		Long seriesId, Integer seriesOrder, Long parentId) {
}
