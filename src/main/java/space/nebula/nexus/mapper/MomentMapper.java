package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface MomentMapper
{

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "likesCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	Moment toEntity(MomentRequest request);

	MomentResponse toResponse(Moment moment);

	List<MomentResponse> toResponseList(List<Moment> moments);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "likesCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget Moment moment, MomentRequest request);
}
