package space.nebula.nexus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanChecklistItem;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.mapper.KanbanMapper;
import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanChecklistItemCompletionRequest;
import space.nebula.nexus.payload.request.KanbanChecklistItemRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.request.KanbanTaskAssigneeRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanChecklistItemResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;
import space.nebula.nexus.repository.KanbanColumnRepository;
import space.nebula.nexus.repository.KanbanChecklistItemRepository;
import space.nebula.nexus.repository.KanbanItemRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IKanbanService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class KanbanServiceImpl implements IKanbanService {
	private static final int MAX_ASSIGNEES_PER_TASK = 20;

	private final KanbanColumnRepository columnRepository;
	private final KanbanItemRepository taskRepository;
	private final KanbanChecklistItemRepository checklistItemRepository;
	private final TagRepository tagRepository;
	private final UserRepository userRepository;
	private final KanbanMapper kanbanMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<KanbanColumnResponse>> retrieveFullBoard() {
		var boardColumns = columnRepository.findAllWithItemsOrderByOrderIndexAsc();
		return ApiResponse.success(kanbanMapper.toColumnResponseList(boardColumns));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanColumnResponse> createColumn(KanbanColumnRequest request) {
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
	public ApiResponse<KanbanColumnResponse> updateColumn(Long id, KanbanColumnRequest request) {
		var column = findColumnOrThrow(id);

		column.setName(request.getName());
		column.setColor(request.getColor());
		var updatedColumn = columnRepository.save(column);
		log.info("Kanban column updated: {}", updatedColumn.getName());
		return ApiResponse.success("Column updated", kanbanMapper.toResponse(updatedColumn));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteColumn(Long id) {
		List<KanbanColumn> columns = columnRepository.findAllForUpdate();
		KanbanColumn column = columns.stream().filter(candidate -> candidate.getId().equals(id)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", id));
		Assert.isTrue(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(id).isEmpty(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Move or delete all tasks before deleting this column"));
		columnRepository.delete(column);
		List<KanbanColumn> remaining = columns.stream().filter(candidate -> !candidate.getId().equals(id))
				.sorted(java.util.Comparator.comparing(KanbanColumn::getOrderIndex).thenComparing(KanbanColumn::getId))
				.toList();
		IntStream.range(0, remaining.size()).forEach(index -> remaining.get(index).setOrderIndex(index));
		columnRepository.saveAll(remaining);
		log.info("Kanban column deleted: {}", id);
		return ApiResponse.success("Column deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> createTask(KanbanItemRequest request) {
		var column = columnRepository.findAllByIdForUpdate(List.of(request.getColumnId())).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", request.getColumnId()));

		var newTask = new KanbanItem();
		newTask.setTitle(request.getTitle());
		newTask.setContent(request.getContent());
		newTask.setPriority(request.getPriority());
		if (request.getEpic() != null && !request.getEpic().isBlank()) {
			newTask.setEpic(request.getEpic().trim());
		}
		if (request.getSize() != null) {
			newTask.setSize(request.getSize());
		}
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

		if (CollUtil.isNotEmpty(request.getTagIds())) {
			var tagIds = new HashSet<>(request.getTagIds());
			var tags = tagRepository.findAllById(tagIds);
			Assert.isTrue(tags.size() == tagIds.size(),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more tags do not exist"));
			newTask.setTags(new HashSet<>(tags));
		}
		if (request.getAssigneeIds() != null) {
			applyTaskAssignees(newTask, request.getAssigneeIds());
		}

		taskRepository.saveAll(existingItems);
		var savedTask = taskRepository.save(newTask);
		log.info("Kanban task created: {}", savedTask.getTitle());
		return ApiResponse.success("Task created", kanbanMapper.toResponse(savedTask));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> updateTask(Long id, KanbanItemRequest request) {
		var task = findItemOrThrow(id);
		Assert.isTrue(task.getColumn().getId().equals(request.getColumnId()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Use the relocate command to change a task column or position"));

		kanbanMapper.updateItem(task, request);

		if (request.getTagIds() != null) {
			var tagIds = new HashSet<>(request.getTagIds());
			var tags = tagRepository.findAllById(tagIds);
			Assert.isTrue(tags.size() == tagIds.size(),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more tags do not exist"));
			task.setTags(new HashSet<>(tags));
		}
		if (request.getAssigneeIds() != null) {
			applyTaskAssignees(task, request.getAssigneeIds());
		}

		var updatedTask = taskRepository.save(task);
		log.info("Kanban task updated: {}", updatedTask.getTitle());
		return ApiResponse.success("Task updated", kanbanMapper.toResponse(updatedTask));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> duplicateTask(Long id) {
		var sourceTask = taskRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
		Long columnId = sourceTask.getColumn().getId();
		var column = columnRepository.findAllByIdForUpdate(List.of(columnId)).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", columnId));
		List<KanbanItem> items = new ArrayList<>(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(columnId));
		int sourceIndex = IntStream.range(0, items.size()).filter(index -> items.get(index).getId().equals(id))
				.findFirst().orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));

		var duplicate = new KanbanItem();
		duplicate.setTitle(StrUtil.subPre(StrUtil.format("Copy of {}", sourceTask.getTitle()), 255));
		duplicate.setContent(sourceTask.getContent());
		duplicate.setPriority(sourceTask.getPriority());
		duplicate.setEpic(sourceTask.getEpic());
		duplicate.setSize(sourceTask.getSize());
		duplicate.setReminderAt(sourceTask.getReminderAt());
		duplicate.setColumn(column);
		duplicate.setTags(new HashSet<>(sourceTask.getTags()));
		duplicate.setAssignees(new HashSet<>(sourceTask.getAssignees()));
		duplicate.setChecklistItems(copyChecklistItems(sourceTask, duplicate));

		items.add(sourceIndex + 1, duplicate);
		renumberItems(items);
		taskRepository.saveAll(items);
		log.info("Kanban task duplicated: {}", sourceTask.getTitle());
		return ApiResponse.success("Task duplicated", kanbanMapper.toResponse(duplicate));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> assignTaskAssignees(Long id, KanbanTaskAssigneeRequest request) {
		KanbanItem task = findItemForUpdateOrThrow(id);
		applyTaskAssignees(task, request.assigneeIds());
		KanbanItem updatedTask = taskRepository.save(task);
		return ApiResponse.success("Task assignees updated", kanbanMapper.toResponse(updatedTask));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<KanbanChecklistItemResponse>> retrieveChecklistItems(Long taskId) {
		findItemOrThrow(taskId);
		List<KanbanChecklistItem> checklistItems = checklistItemRepository
				.findByTaskIdOrderByOrderIndexAscIdAsc(taskId);
		return ApiResponse.success(kanbanMapper.toChecklistItemResponseList(checklistItems));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanChecklistItemResponse> createChecklistItem(Long taskId,
			KanbanChecklistItemRequest request) {
		KanbanItem task = findItemForUpdateOrThrow(taskId);
		List<KanbanChecklistItem> checklistItems = checklistItemsForTask(taskId);
		int insertIndex = request.orderIndex() == null
				? checklistItems.size()
				: Math.max(0, Math.min(request.orderIndex(), checklistItems.size()));

		KanbanChecklistItem checklistItem = new KanbanChecklistItem();
		checklistItem.setTitle(request.title());
		checklistItem.setCompleted(request.completed());
		checklistItem.setTask(task);
		checklistItems.add(insertIndex, checklistItem);
		renumberChecklistItems(checklistItems);
		checklistItemRepository.saveAll(checklistItems);

		return ApiResponse.success("Checklist item created", kanbanMapper.toChecklistItemResponse(checklistItem));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanChecklistItemResponse> updateChecklistItem(Long taskId, Long checklistItemId,
			KanbanChecklistItemRequest request) {
		findItemForUpdateOrThrow(taskId);
		List<KanbanChecklistItem> checklistItems = checklistItemsForTask(taskId);
		KanbanChecklistItem checklistItem = findChecklistItemOrThrow(taskId, checklistItemId, checklistItems);
		checklistItem.setTitle(request.title());
		checklistItem.setCompleted(request.completed());

		if (request.orderIndex() != null) {
			checklistItems.remove(checklistItem);
			checklistItems.add(Math.max(0, Math.min(request.orderIndex(), checklistItems.size())), checklistItem);
			renumberChecklistItems(checklistItems);
			checklistItemRepository.saveAll(checklistItems);
		} else {
			checklistItemRepository.save(checklistItem);
		}

		return ApiResponse.success("Checklist item updated", kanbanMapper.toChecklistItemResponse(checklistItem));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanChecklistItemResponse> completeChecklistItem(Long taskId, Long checklistItemId,
			KanbanChecklistItemCompletionRequest request) {
		findItemForUpdateOrThrow(taskId);
		KanbanChecklistItem checklistItem = findChecklistItemOrThrow(taskId, checklistItemId,
				checklistItemsForTask(taskId));
		checklistItem.setCompleted(request.completed());
		KanbanChecklistItem savedChecklistItem = checklistItemRepository.save(checklistItem);
		return ApiResponse.success("Checklist item completion updated",
				kanbanMapper.toChecklistItemResponse(savedChecklistItem));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteChecklistItem(Long taskId, Long checklistItemId) {
		findItemForUpdateOrThrow(taskId);
		List<KanbanChecklistItem> checklistItems = checklistItemsForTask(taskId);
		KanbanChecklistItem checklistItem = findChecklistItemOrThrow(taskId, checklistItemId, checklistItems);
		checklistItems.remove(checklistItem);
		checklistItemRepository.delete(checklistItem);
		renumberChecklistItems(checklistItems);
		checklistItemRepository.saveAll(checklistItems);
		return ApiResponse.success("Checklist item deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> adjustChecklistItemSequence(Long taskId, List<Long> checklistItemIds) {
		findItemForUpdateOrThrow(taskId);
		Assert.isTrue(checklistItemIds != null,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Checklist item IDs are required"));
		Assert.isTrue(new HashSet<>(checklistItemIds).size() == checklistItemIds.size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Checklist item IDs must not contain duplicates"));
		List<KanbanChecklistItem> checklistItems = checklistItemsForTask(taskId);
		Assert.isTrue(
				checklistItems.size() == checklistItemIds.size()
						&& checklistItems.stream().map(KanbanChecklistItem::getId)
								.collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(checklistItemIds)),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Checklist sequence must contain every task checklist item exactly once"));
		var checklistById = checklistItems.stream()
				.collect(java.util.stream.Collectors.toMap(KanbanChecklistItem::getId, checklistItem -> checklistItem));
		IntStream.range(0, checklistItemIds.size())
				.forEach(index -> checklistById.get(checklistItemIds.get(index)).setOrderIndex(index));
		checklistItemRepository.saveAll(checklistItems);
		return ApiResponse.success("Checklist sequence adjusted", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteTask(Long id) {
		KanbanItem task = taskRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
		Long columnId = task.getColumn().getId();
		columnRepository.findAllByIdForUpdate(List.of(columnId));
		List<KanbanItem> remaining = new ArrayList<>(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(columnId));
		remaining.removeIf(candidate -> candidate.getId().equals(id));
		checklistItemRepository.deleteAll(checklistItemsForTask(id));
		taskRepository.delete(task);
		renumberItems(remaining);
		taskRepository.saveAll(remaining);
		log.info("Kanban task deleted: {}", id);
		return ApiResponse.success("Task deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> relocateTask(KanbanItemMoveRequest request) {
		Assert.isTrue(request.getTargetOrderIndex() != null && request.getTargetOrderIndex() >= 0,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Target order index cannot be negative"));
		var task = taskRepository.findByIdForUpdate(request.getItemId())
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", request.getItemId()));
		Long sourceColumnId = task.getColumn().getId();
		Long targetColumnId = request.getTargetColumnId();
		var lockedColumns = columnRepository
				.findAllByIdForUpdate(new LinkedHashSet<>(List.of(sourceColumnId, targetColumnId)));
		Assert.isTrue(lockedColumns.size() == (sourceColumnId.equals(targetColumnId) ? 1 : 2),
				() -> new ResourceNotFoundException("KanbanColumn", "id", targetColumnId));
		KanbanColumn targetColumn = lockedColumns.stream().filter(column -> column.getId().equals(targetColumnId))
				.findFirst().orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", targetColumnId));

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
		if (targetItems != sourceItems) {
			renumberItems(targetItems);
			taskRepository.saveAll(sourceItems);
		}
		taskRepository.saveAll(targetItems);

		return ApiResponse.success("Task relocated", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> adjustColumnSequence(List<Long> columnIds) {
		Assert.notEmpty(columnIds, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Column IDs are required"));
		Assert.isTrue(new HashSet<>(columnIds).size() == columnIds.size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Column IDs must not contain duplicates"));
		List<KanbanColumn> columns = columnRepository.findAllForUpdate();
		Assert.isTrue(
				columns.size() == columnIds.size() && columns.stream().map(KanbanColumn::getId)
						.collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(columnIds)),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Column sequence must contain every board column exactly once"));
		var byId = columns.stream().collect(java.util.stream.Collectors.toMap(KanbanColumn::getId, column -> column));
		IntStream.range(0, columnIds.size()).forEach(i -> byId.get(columnIds.get(i)).setOrderIndex(i));
		columnRepository.saveAll(columns);
		return ApiResponse.success("Sequence adjusted", null);
	}

	private void renumberItems(List<KanbanItem> items) {
		IntStream.range(0, items.size()).forEach(index -> items.get(index).setOrderIndex(index));
	}

	private void renumberChecklistItems(List<KanbanChecklistItem> checklistItems) {
		IntStream.range(0, checklistItems.size()).forEach(index -> checklistItems.get(index).setOrderIndex(index));
	}

	private List<KanbanChecklistItem> checklistItemsForTask(Long taskId) {
		return new ArrayList<>(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(taskId));
	}

	private KanbanChecklistItem findChecklistItemOrThrow(Long taskId, Long checklistItemId,
			List<KanbanChecklistItem> checklistItems) {
		return checklistItems.stream().filter(checklistItem -> checklistItem.getId().equals(checklistItemId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("KanbanChecklistItem", "id", checklistItemId));
	}

	private List<KanbanChecklistItem> copyChecklistItems(KanbanItem sourceTask, KanbanItem duplicateTask) {
		List<KanbanChecklistItem> duplicateChecklistItems = new ArrayList<>();
		for (KanbanChecklistItem sourceChecklistItem : sourceTask.getChecklistItems()) {
			KanbanChecklistItem duplicateChecklistItem = new KanbanChecklistItem();
			duplicateChecklistItem.setTitle(sourceChecklistItem.getTitle());
			duplicateChecklistItem.setCompleted(sourceChecklistItem.getCompleted());
			duplicateChecklistItem.setOrderIndex(sourceChecklistItem.getOrderIndex());
			duplicateChecklistItem.setTask(duplicateTask);
			duplicateChecklistItems.add(duplicateChecklistItem);
		}
		return duplicateChecklistItems;
	}

	private void applyTaskAssignees(KanbanItem task, Set<Long> assigneeIds) {
		Assert.isTrue(assigneeIds != null,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Assignee IDs are required"));
		Assert.isTrue(assigneeIds.stream().noneMatch(java.util.Objects::isNull),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Assignee IDs must not contain null values"));
		if (assigneeIds.isEmpty()) {
			task.setAssignees(new HashSet<>());
			return;
		}
		Set<Long> uniqueAssigneeIds = new HashSet<>(assigneeIds);
		Assert.isTrue(uniqueAssigneeIds.size() <= MAX_ASSIGNEES_PER_TASK,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A task can have at most 20 assignees"));
		List<User> assignees = userRepository.findAllById(uniqueAssigneeIds);
		Assert.isTrue(assignees.size() == uniqueAssigneeIds.size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more assignees do not exist"));
		Assert.isTrue(assignees.stream().allMatch(assignee -> assignee.getStatus() == UserStatus.ACTIVE),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "All task assignees must have active accounts"));
		task.setAssignees(new HashSet<>(assignees));
	}

	private KanbanColumn findColumnOrThrow(Long id) {
		return columnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", id));
	}

	private KanbanItem findItemOrThrow(Long id) {
		return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
	}

	private KanbanItem findItemForUpdateOrThrow(Long id) {
		return taskRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));
	}
}
