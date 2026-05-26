package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class, uses = { CategoryMapper.class, TagMapper.class, PostSeriesMapper.class })
public interface PostMapper
{

	@Mapping(target = "authorName", source = "author", qualifiedByName = "mapAuthorName")
	@Mapping(target = "series", source = "series", qualifiedByName = "toResponse")
	@Mapping(target = "isLiked", ignore = true)
	@Mapping(target = "isFavorited", ignore = true)
	PostResponse toResponse(Post post);

	List<PostResponse> toResponseList(List<Post> posts);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "series", ignore = true)
	@Mapping(target = "seriesOrder", ignore = true)
	@Mapping(target = "tags", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "views", ignore = true)
	@Mapping(target = "likesCount", ignore = true)
	@Mapping(target = "favoritesCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "publishedAt", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget Post post, PostRequest request);

	@Named("mapAuthorName")
	default String mapAuthorName(User author)
	{
		if (author == null)
			return null;
		return author.getNickname() != null ? author.getNickname() : author.getUsername();
	}
}
