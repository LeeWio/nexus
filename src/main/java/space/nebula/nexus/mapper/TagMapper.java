package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface TagMapper
{

	TagResponse toResponse(Tag tag);

	List<TagResponse> toResponseList(List<Tag> tags);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget Tag tag, TagRequest request);
}
