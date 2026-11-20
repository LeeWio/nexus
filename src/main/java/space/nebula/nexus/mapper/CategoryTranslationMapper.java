package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.CategoryTranslation;
import space.nebula.nexus.mapper.config.CentralMapperConfig;
import space.nebula.nexus.payload.response.CategoryTranslationResponse;

@Mapper(config = CentralMapperConfig.class)
public interface CategoryTranslationMapper {
	CategoryTranslationResponse toResponse(CategoryTranslation translation);
}
