package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Config;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ConfigMapper {

    ConfigResponse toResponse(Config config);

    List<ConfigResponse> toResponseList(List<Config> configs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Config toEntity(ConfigRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateEntity(@MappingTarget Config config, ConfigRequest request);
}
