package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.payload.response.CommentResponse;

import java.util.List;

@Mapper(componentModel = "spring", uses = UserAvatarMapper.class)
public interface CommentMapper {

	@Mapping(target = "authorUserId", source = "user.id")
	@Mapping(target = "username", source = "user.username")
	@Mapping(target = "nickname", source = "user.nickname")
	@Mapping(target = "avatar", source = "user", qualifiedByName = "resolveUserAvatar")
	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "postId", source = "post.id")
	@Mapping(target = "postTitle", source = "post.title")
	@Mapping(target = "momentId", source = "moment.id")
	@Mapping(target = "replyCount", ignore = true)
	@Mapping(target = "likedByCurrentUser", ignore = true)
	@Mapping(target = "viewerCanEdit", ignore = true)
	@Mapping(target = "viewerCanDelete", ignore = true)
	CommentResponse toResponse(Comment comment);

	List<CommentResponse> toResponseList(List<Comment> comments);
}
