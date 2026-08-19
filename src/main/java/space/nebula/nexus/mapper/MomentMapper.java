package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.MomentMedia;
import space.nebula.nexus.entity.MomentTopicRelation;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentImageResponse;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.MomentTopicResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface MomentMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "likesCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "images", ignore = true)
	@Mapping(target = "topicRelations", ignore = true)
	@Mapping(target = "user", ignore = true)
	Moment toEntity(MomentRequest request);

	default MomentResponse toResponse(Moment moment) {
		List<MomentImageResponse> images = moment.getImages() == null
				? List.of()
				: moment.getImages().stream().sorted(java.util.Comparator.comparing(MomentMedia::getSortOrder))
						.map(this::toImageResponse).toList();
		List<MomentTopicResponse> topics = moment.getTopicRelations() == null
				? List.of()
				: moment.getTopicRelations().stream()
						.sorted(java.util.Comparator.comparing(MomentTopicRelation::getSortOrder))
						.map(this::toTopicResponse).toList();
						
		String authorName = null;
		String authorAvatar = null;
		if (moment.getUser() != null) {
			authorName = moment.getUser().getNickname() != null ? moment.getUser().getNickname() : moment.getUser().getUsername();
			authorAvatar = moment.getUser().getAvatar();
		}
		
		return new MomentResponse(moment.getId(), moment.getContent(), moment.getLikesCount(), moment.getVisibility(),
				authorName, authorAvatar, images, topics, moment.getCreatedAt(), moment.getUpdatedAt());
	}

	default MomentImageResponse toImageResponse(MomentMedia media) {
		var file = media.getFile();
		return new MomentImageResponse(media.getId(), file.getId(), file.getOriginalName(), file.getFileUrl(),
				file.getThumbnailUrl(), file.getWidth(), file.getHeight(), media.getAltText(), media.getSortOrder());
	}

	default MomentTopicResponse toTopicResponse(MomentTopicRelation relation) {
		var topic = relation.getTopic();
		return new MomentTopicResponse(topic.getId(), topic.getSlug());
	}

	List<MomentResponse> toResponseList(List<Moment> moments);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "likesCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "images", ignore = true)
	@Mapping(target = "topicRelations", ignore = true)
	@Mapping(target = "user", ignore = true)
	void updateEntity(@MappingTarget Moment moment, MomentRequest request);
}
