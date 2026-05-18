package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;

@Mapper(config = CentralMapperConfig.class, uses = {TagMapper.class})
public interface KanbanMapper {

    KanbanColumnResponse toResponse(KanbanColumn column);

    List<KanbanColumnResponse> toColumnResponseList(List<KanbanColumn> columns);

    @Mapping(target = "columnId", source = "column.id")
    KanbanItemResponse toResponse(KanbanItem item);

    List<KanbanItemResponse> toItemResponseList(List<KanbanItem> items);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "column", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateItem(@MappingTarget KanbanItem entity, KanbanItemRequest request);
}
