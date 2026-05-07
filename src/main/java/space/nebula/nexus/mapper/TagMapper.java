package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.payload.response.TagResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponse toResponse(Tag tag);
    List<TagResponse> toResponseList(List<Tag> tags);
}
