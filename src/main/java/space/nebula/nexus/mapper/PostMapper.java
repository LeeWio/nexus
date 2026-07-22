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
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class, uses = { CategoryMapper.class, TagMapper.class, PostSeriesMapper.class })
public interface PostMapper
{

	@Mapping(target = "authorName", source = "author", qualifiedByName = "mapAuthorName")
	@Mapping(target = "authorAvatar", source = "author.avatar")
	@Mapping(target = "series", source = "series", qualifiedByName = "toResponse")
	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "reviewerName", source = "reviewedBy", qualifiedByName = "mapAuthorName")
	@Mapping(target = "archivedByName", source = "archivedBy", qualifiedByName = "mapAuthorName")
	@Mapping(target = "breadcrumbs", ignore = true)
	@Mapping(target = "seo", ignore = true)
	@Mapping(target = "navigation", ignore = true)
	@Mapping(target = "isLiked", ignore = true)
	@Mapping(target = "isFavorited", ignore = true)
	PostResponse toResponse(Post post);

	List<PostResponse> toResponseList(List<Post> posts);

	@Mapping(target = "authorName", source = "author", qualifiedByName = "mapAuthorName")
	@Mapping(target = "authorAvatar", source = "author.avatar")
	PostDigestResponse toDigestResponse(Post post);

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
	@Mapping(target = "autoSummary", ignore = true)
	@Mapping(target = "wordCount", ignore = true)
	@Mapping(target = "readingTimeMinutes", ignore = true)
	@Mapping(target = "contentHash", ignore = true)
	@Mapping(target = "toc", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "publishedAt", ignore = true)
	@Mapping(target = "scheduledAt", ignore = true)
	@Mapping(target = "reviewComment", ignore = true)
	@Mapping(target = "reviewedAt", ignore = true)
	@Mapping(target = "reviewedBy", ignore = true)
	@Mapping(target = "archiveReason", ignore = true)
	@Mapping(target = "archivedAt", ignore = true)
	@Mapping(target = "archivedBy", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "parent", ignore = true)
	@Mapping(target = "path", ignore = true)
	void updateEntity(@MappingTarget Post post, PostRequest request);

	@Named("mapAuthorName")
	default String mapAuthorName(User author)
	{
		if (author == null)
			return null;
		return author.getNickname() != null ? author.getNickname() : author.getUsername();
	}
}
