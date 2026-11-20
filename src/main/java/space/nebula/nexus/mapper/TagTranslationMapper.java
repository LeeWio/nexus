package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.TagTranslation;
import space.nebula.nexus.mapper.config.CentralMapperConfig;
import space.nebula.nexus.payload.response.TagTranslationResponse;

@Mapper(config = CentralMapperConfig.class)
public interface TagTranslationMapper {
	TagTranslationResponse toResponse(TagTranslation translation);
}
