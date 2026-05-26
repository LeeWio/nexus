package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface PostRevisionMapper
{

	@Mapping(target = "createdBy", source = "createdBy.username")
	@Mapping(target = "postId", source = "post.id")
	PostRevisionResponse toResponse(PostRevision revision);

	List<PostRevisionResponse> toResponseList(List<PostRevision> revisions);
}
