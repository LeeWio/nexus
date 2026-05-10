package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MomentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "likesCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Moment toEntity(MomentRequest request);

    MomentResponse toResponse(Moment moment);

    List<MomentResponse> toResponseList(List<Moment> moments);
}
