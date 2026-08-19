package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import space.nebula.nexus.enums.CommentStatus;
import java.time.LocalDateTime;

/**
 * Response DTO for displaying comments. Kept flat as TreeUtil handles the
 * hierarchy.
 */
@Builder
@Schema(description = "Comment or guestbook entry. Replies refer to their direct parent through parentId.")
public record CommentResponse(@Schema(description = "Comment ID") Long id,
		@Schema(description = "Direct parent comment ID; null for a root comment") Long parentId,
		@Schema(description = "Comment content, or a deletion placeholder when deletedPlaceholder is true") String content,
		@Schema(description = "Author username") String username,
		@Schema(description = "Author display name") String nickname,
		@Schema(description = "Author avatar URL") String avatar,
		@Schema(description = "Moderation status") CommentStatus status,
		@Schema(description = "Associated post ID; null for guestbook entries") Long postId,
		@Schema(description = "Associated post title; null for guestbook entries") String postTitle,
		@Schema(description = "Visible like count") Long likesCount,
		@Schema(description = "Open report count, exposed to moderation views") Long reportsCount,
		@Schema(description = "Number of direct replies") Integer replyCount,
		@Schema(description = "Whether the authenticated caller liked this comment; null for anonymous callers") Boolean likedByCurrentUser,
		@Schema(description = "Whether the comment is pinned before normal ordering") Boolean pinned,
		@Schema(description = "Whether the comment is editorially featured") Boolean featured,
		@Schema(description = "Whether content is a retained placeholder after a parent comment was deleted") Boolean deletedPlaceholder,
		@Schema(description = "Creation time") LocalDateTime createdAt,
		@Schema(description = "Most recent author edit time; null when never edited") LocalDateTime editedAt) {
}
