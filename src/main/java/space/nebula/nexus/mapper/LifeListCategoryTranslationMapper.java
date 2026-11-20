package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.LifeListCategoryTranslation;
import space.nebula.nexus.mapper.config.CentralMapperConfig;
import space.nebula.nexus.payload.response.LifeListCategoryTranslationResponse;

@Mapper(config = CentralMapperConfig.class)
public interface LifeListCategoryTranslationMapper {
	LifeListCategoryTranslationResponse toResponse(LifeListCategoryTranslation translation);
}
