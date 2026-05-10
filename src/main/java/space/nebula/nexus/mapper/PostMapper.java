package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.PostResponse;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, TagMapper.class})
public interface PostMapper {

    @Mapping(target = "authorName", source = "author", qualifiedByName = "mapAuthorName")
    @Mapping(target = "isLiked", ignore = true)
    @Mapping(target = "isFavorited", ignore = true)
    PostResponse toResponse(Post post);

    List<PostResponse> toResponseList(List<Post> posts);

    @Named("mapAuthorName")
    default String mapAuthorName(User author) {
        if (author == null) return null;
        return author.getNickname() != null ? author.getNickname() : author.getUsername();
    }
}
