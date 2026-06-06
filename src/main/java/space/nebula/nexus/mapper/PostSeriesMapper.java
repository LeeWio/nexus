package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.payload.request.SeriesRequest;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public abstract class PostSeriesMapper
{

	protected PostMapper postMapper;

	@org.springframework.beans.factory.annotation.Autowired
	public void setPostMapper(PostMapper postMapper) {
		this.postMapper = postMapper;
	}

	@Named("toResponse")
	@Mapping(target = "postsCount", expression = "java(series.getPosts() != null ? series.getPosts().size() : 0)")
	@Mapping(target = "posts", ignore = true)
	public abstract SeriesResponse toResponse(PostSeries series);

	@Mapping(target = "postsCount", expression = "java(series.getPosts() != null ? series.getPosts().size() : 0)")
	@Mapping(target = "posts", expression = "java(postMapper.toResponseList(series.getPosts()))")
	public abstract SeriesResponse toResponseWithPosts(PostSeries series);

	@IterableMapping(qualifiedByName = "toResponse")
	public abstract List<SeriesResponse> toResponseList(List<PostSeries> seriesList);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "posts", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	public abstract void updateEntity(@MappingTarget PostSeries series, SeriesRequest request);
}
