package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.payload.response.CommentResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper
{

	@Mapping(target = "username", source = "user.username")
	@Mapping(target = "nickname", source = "user.nickname")
	@Mapping(target = "avatar", source = "user.avatar")
	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "postId", source = "post.id")
	@Mapping(target = "postTitle", source = "post.title")
	CommentResponse toResponse(Comment comment);

	List<CommentResponse> toResponseList(List<Comment> comments);
}
