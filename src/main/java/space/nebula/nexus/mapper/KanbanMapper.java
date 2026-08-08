package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanChecklistItem;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanAssigneeResponse;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanChecklistItemResponse;
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

	@Mapping(target = "taskId", source = "task.id")
	KanbanChecklistItemResponse toChecklistItemResponse(KanbanChecklistItem item);

	List<KanbanChecklistItemResponse> toChecklistItemResponseList(List<KanbanChecklistItem> items);

	KanbanAssigneeResponse toAssigneeResponse(User user);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "column", ignore = true)
	@Mapping(target = "orderIndex", ignore = true)
	@Mapping(target = "tags", ignore = true)
	@Mapping(target = "assignees", ignore = true)
	@Mapping(target = "checklistItems", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateItem(@MappingTarget KanbanItem entity, KanbanItemRequest request);
}
