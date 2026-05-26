package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.payload.request.CategoryRequest;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface CategoryMapper
{

	CategoryResponse toResponse(Category category);

	List<CategoryResponse> toResponseList(List<Category> categories);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget Category category, CategoryRequest request);
}
