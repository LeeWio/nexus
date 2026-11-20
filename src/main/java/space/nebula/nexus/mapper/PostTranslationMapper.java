package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.PostTranslation;
import space.nebula.nexus.mapper.config.CentralMapperConfig;
import space.nebula.nexus.payload.response.PostDigestTranslationResponse;
import space.nebula.nexus.payload.response.PostTranslationResponse;

@Mapper(config = CentralMapperConfig.class)
public interface PostTranslationMapper {
	PostTranslationResponse toResponse(PostTranslation translation);

	PostDigestTranslationResponse toDigestResponse(PostTranslation translation);
}
