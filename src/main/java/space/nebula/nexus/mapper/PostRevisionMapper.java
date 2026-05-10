package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.payload.response.PostRevisionResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PostRevisionMapper {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "createdBy", source = "createdBy.username")
    PostRevisionResponse toResponse(PostRevision revision);

    List<PostRevisionResponse> toResponseList(List<PostRevision> revisions);
}
