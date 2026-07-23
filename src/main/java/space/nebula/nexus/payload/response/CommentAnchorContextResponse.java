package space.nebula.nexus.payload.response;

import lombok.Builder;

@Builder
public record CommentAnchorContextResponse(Long rootCommentId, CommentResponse rootComment,
		CommentResponse targetComment, PageResult<CommentResponse> repliesWindow) {
}
