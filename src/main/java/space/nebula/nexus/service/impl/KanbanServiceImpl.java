package space.nebula.nexus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.mapper.KanbanMapper;
import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;
import space.nebula.nexus.repository.KanbanColumnRepository;
import space.nebula.nexus.repository.KanbanItemRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.service.IKanbanService;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.LinkedHashSet;
import java.util.ArrayList;

/**
 * Implementation of Kanban board management service. Standardized with
 * ApiResponse and enhanced with concurrency protection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanServiceImpl implements IKanbanService
{

	private final KanbanColumnRepository columnRepository;
	private final KanbanItemRepository taskRepository;
	private final TagRepository tagRepository;
	private final KanbanMapper kanbanMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<KanbanColumnResponse>> retrieveFullBoard()
	{
		var boardColumns = columnRepository.findAllWithItemsOrderByOrderIndexAsc();
		return ApiResponse.success(kanbanMapper.toColumnResponseList(boardColumns));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanColumnResponse> createColumn(KanbanColumnRequest request)
	{
		List<KanbanColumn> existingColumns = columnRepository.findAllForUpdate();
		var newColumn = new KanbanColumn();
		newColumn.setName(request.getName());
		newColumn.setColor(request.getColor());

		newColumn.setOrderIndex(existingColumns.size());

		var savedColumn = columnRepository.save(newColumn);
		log.info("Kanban column created: {}", savedColumn.getName());
		return ApiResponse.success("Column created", kanbanMapper.toResponse(savedColumn));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanColumnResponse> updateColumn(Long id, KanbanColumnRequest request)
	{
		var column = findColumnOrThrow(id);

		column.setName(request.getName());
		column.setColor(request.getColor());
		var updatedColumn = columnRepository.save(column);
		log.info("Kanban column updated: {}", updatedColumn.getName());
		return ApiResponse.success("Column updated", kanbanMapper.toResponse(updatedColumn));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteColumn(Long id)
	{
		List<KanbanColumn> columns = columnRepository.findAllForUpdate();
		KanbanColumn column = columns.stream().filter(candidate -> candidate.getId().equals(id)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", id));
		Assert.isTrue(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(id).isEmpty(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Move or delete all tasks before deleting this column"));
		columnRepository.delete(column);
		List<KanbanColumn> remaining = columns.stream().filter(candidate -> !candidate.getId().equals(id))
				.sorted(java.util.Comparator.comparing(KanbanColumn::getOrderIndex)
						.thenComparing(KanbanColumn::getId)).toList();
		IntStream.range(0, remaining.size()).forEach(index -> remaining.get(index).setOrderIndex(index));
		columnRepository.saveAll(remaining);
		log.info("Kanban column deleted: {}", id);
		return ApiResponse.success("Column deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> createTask(KanbanItemRequest request)
	{
		var column = columnRepository.findAllByIdForUpdate(List.of(request.getColumnId())).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", request.getColumnId()));

		var newTask = new KanbanItem();
		newTask.setTitle(request.getTitle());
		newTask.setContent(request.getContent());
		newTask.setPriority(request.getPriority());
		newTask.setReminderAt(request.getReminderAt());
		newTask.setColumn(column);

		List<KanbanItem> existingItems = new ArrayList<>(
				taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(column.getId()));
		int insertIndex = request.getOrderIndex() == null
				? existingItems.size()
				: Math.max(0, Math.min(request.getOrderIndex(), existingItems.size()));
		newTask.setOrderIndex(insertIndex);
		existingItems.stream().filter(item -> item.getOrderIndex() >= insertIndex)
				.forEach(item -> item.setOrderIndex(item.getOrderIndex() + 1));

		if (CollUtil.isNotEmpty(request.getTagIds()))
		{
			var tagIds = new HashSet<>(request.getTagIds());
			var tags = tagRepository.findAllById(tagIds);
			Assert.isTrue(tags.size() == tagIds.size(),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more tags do not exist"));
			newTask.setTags(new HashSet<>(tags));
		}

		taskRepository.saveAll(existingItems);
		var savedTask = taskRepository.save(newTask);
		log.info("Kanban task created: {}", savedTask.getTitle());
		return ApiResponse.success("Task created", kanbanMapper.toResponse(savedTask));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> updateTask(Long id, KanbanItemRequest request)
	{
		var task = findItemOrThrow(id);
		Assert.isTrue(task.getColumn().getId().equals(request.getColumnId()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Use the relocate command to change a task column or position"));

		kanbanMapper.updateItem(task, request);

		if (request.getTagIds() != null)
		{
			var tagIds = new HashSet<>(request.getTagIds());
			var tags = tagRepository.findAllById(tagIds);
			Assert.isTrue(tags.size() == tagIds.size(),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more tags do not exist"));
			task.setTags(new HashSet<>(tags));
		}

		var updatedTask = taskRepository.save(task);
		log.info("Kanban task updated: {}", updatedTask.getTitle());
		return ApiResponse.success("Task updated", kanbanMapper.toResponse(updatedTask));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteTask(Long id)
	{
		KanbanItem task = taskRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
		Long columnId = task.getColumn().getId();
		columnRepository.findAllByIdForUpdate(List.of(columnId));
		List<KanbanItem> remaining = new ArrayList<>(
				taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(columnId));
		remaining.removeIf(candidate -> candidate.getId().equals(id));
		taskRepository.delete(task);
		renumberItems(remaining);
		taskRepository.saveAll(remaining);
		log.info("Kanban task deleted: {}", id);
		return ApiResponse.success("Task deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> relocateTask(KanbanItemMoveRequest request)
	{
		Assert.isTrue(request.getTargetOrderIndex() != null && request.getTargetOrderIndex() >= 0,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Target order index cannot be negative"));
		var task = taskRepository.findByIdForUpdate(request.getItemId())
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", request.getItemId()));
		Long sourceColumnId = task.getColumn().getId();
		Long targetColumnId = request.getTargetColumnId();
		var lockedColumns = columnRepository.findAllByIdForUpdate(
				new LinkedHashSet<>(List.of(sourceColumnId, targetColumnId)));
		Assert.isTrue(lockedColumns.size() == (sourceColumnId.equals(targetColumnId) ? 1 : 2),
				() -> new ResourceNotFoundException("KanbanColumn", "id", targetColumnId));
		KanbanColumn targetColumn = lockedColumns.stream()
				.filter(column -> column.getId().equals(targetColumnId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", targetColumnId));

		List<KanbanItem> sourceItems = new ArrayList<>(
				taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(sourceColumnId));
		sourceItems.removeIf(item -> item.getId().equals(task.getId()));
		List<KanbanItem> targetItems = sourceColumnId.equals(targetColumnId)
				? sourceItems
				: new ArrayList<>(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(targetColumnId));

		int targetIndex = Math.min(request.getTargetOrderIndex(), targetItems.size());
		task.setColumn(targetColumn);
		targetItems.add(targetIndex, task);
		renumberItems(sourceItems);
		if (targetItems != sourceItems)
		{
			renumberItems(targetItems);
			taskRepository.saveAll(sourceItems);
		}
		taskRepository.saveAll(targetItems);

		return ApiResponse.success("Task relocated", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> adjustColumnSequence(List<Long> columnIds)
	{
		Assert.notEmpty(columnIds, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Column IDs are required"));
		Assert.isTrue(new HashSet<>(columnIds).size() == columnIds.size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Column IDs must not contain duplicates"));
		List<KanbanColumn> columns = columnRepository.findAllForUpdate();
		Assert.isTrue(columns.size() == columnIds.size()
				&& columns.stream().map(KanbanColumn::getId).collect(java.util.stream.Collectors.toSet())
						.equals(new HashSet<>(columnIds)),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Column sequence must contain every board column exactly once"));
		var byId = columns.stream().collect(java.util.stream.Collectors.toMap(KanbanColumn::getId, column -> column));
		IntStream.range(0, columnIds.size()).forEach(i -> byId.get(columnIds.get(i)).setOrderIndex(i));
		columnRepository.saveAll(columns);
		return ApiResponse.success("Sequence adjusted", null);
	}

	private void renumberItems(List<KanbanItem> items)
	{
		IntStream.range(0, items.size()).forEach(index -> items.get(index).setOrderIndex(index));
	}

	private KanbanColumn findColumnOrThrow(Long id)
	{
		return columnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", id));
	}

	private KanbanItem findItemOrThrow(Long id)
	{
		return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
	}
}
