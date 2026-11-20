package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.LifeListCategory;
import space.nebula.nexus.mapper.config.CentralMapperConfig;
import space.nebula.nexus.payload.response.LifeListCategoryResponse;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class, uses = LifeListCategoryTranslationMapper.class)
public interface LifeListCategoryMapper {
	@Mapping(target = "translations", source = "translations")
	LifeListCategoryResponse toResponse(LifeListCategory category);
}
