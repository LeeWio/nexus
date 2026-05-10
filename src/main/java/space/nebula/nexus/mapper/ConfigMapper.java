package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import space.nebula.nexus.entity.Config;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConfigMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Config toEntity(ConfigRequest request);
    
    ConfigResponse toResponse(Config config);
    List<ConfigResponse> toResponseList(List<Config> configs);
}
