package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
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
import space.nebula.nexus.utils.RedisLockUtil;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
	private final RedisLockUtil redisLockUtil;

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
		var newColumn = new KanbanColumn();
		newColumn.setName(request.getName());
		newColumn.setColor(request.getColor());

		if (request.getOrderIndex() != null)
		{
			newColumn.setOrderIndex(request.getOrderIndex());
		}
		else
		{
			Integer maxSequence = columnRepository.findMaxOrderIndex();
			newColumn.setOrderIndex(maxSequence != null ? maxSequence + 1 : 0);
		}

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
		if (request.getOrderIndex() != null)
		{
			column.setOrderIndex(request.getOrderIndex());
		}

		var updatedColumn = columnRepository.save(column);
		log.info("Kanban column updated: {}", updatedColumn.getName());
		return ApiResponse.success("Column updated", kanbanMapper.toResponse(updatedColumn));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteColumn(Long id)
	{
		if (!columnRepository.existsById(id))
		{
			throw new ResourceNotFoundException("KanbanColumn", "id", id);
		}
		columnRepository.deleteById(id);
		log.info("Kanban column deleted: {}", id);
		return ApiResponse.success("Column deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> createTask(KanbanItemRequest request)
	{
		var column = findColumnOrThrow(request.getColumnId());

		var newTask = new KanbanItem();
		newTask.setTitle(request.getTitle());
		newTask.setContent(request.getContent());
		newTask.setPriority(request.getPriority());
		newTask.setReminderAt(request.getReminderAt());
		newTask.setColumn(column);

		if (request.getOrderIndex() != null)
		{
			newTask.setOrderIndex(request.getOrderIndex());
		}
		else
		{
			Integer maxSequence = taskRepository.findMaxOrderIndexByColumnId(column.getId());
			newTask.setOrderIndex(maxSequence != null ? maxSequence + 1 : 0);
		}

		if (request.getTagIds() != null && !request.getTagIds().isEmpty())
		{
			var tags = tagRepository.findAllById(request.getTagIds());
			newTask.setTags(new HashSet<>(tags));
		}

		var savedTask = taskRepository.save(newTask);
		log.info("Kanban task created: {}", savedTask.getTitle());
		return ApiResponse.success("Task created", kanbanMapper.toResponse(savedTask));
	}

	@Override
	@Transactional
	public ApiResponse<KanbanItemResponse> updateTask(Long id, KanbanItemRequest request)
	{
		var task = findItemOrThrow(id);

		kanbanMapper.updateItem(task, request);

		if (request.getColumnId() != null && !task.getColumn().getId().equals(request.getColumnId()))
		{
			var targetColumn = findColumnOrThrow(request.getColumnId());
			task.setColumn(targetColumn);
		}

		if (request.getTagIds() != null)
		{
			var tags = tagRepository.findAllById(request.getTagIds());
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
		if (!taskRepository.existsById(id))
		{
			throw new ResourceNotFoundException("KanbanItem", "id", id);
		}
		taskRepository.deleteById(id);
		log.info("Kanban task deleted: {}", id);
		return ApiResponse.success("Task deleted", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> relocateTask(KanbanItemMoveRequest request)
	{
		String lockKey = CacheConstants.LOCK_KANBAN_COLUMN_PREFIX + request.getTargetColumnId();
		String lockToken = UUID.randomUUID().toString();

		if (!redisLockUtil.tryLock(lockKey, lockToken, 5, TimeUnit.SECONDS))
		{
			throw new BusinessException(BusinessCode.ERROR, "Board is busy, please try again");
		}

		try
		{
			var task = findItemOrThrow(request.getItemId());
			var destColumn = findColumnOrThrow(request.getTargetColumnId());

			task.setColumn(destColumn);
			task.setOrderIndex(request.getTargetOrderIndex());
			taskRepository.save(task);

			var tasks = destColumn.getItems();
			if (!tasks.contains(task))
			{
				tasks.add(task);
			}

			tasks.sort((a, b) ->
			{
				if (a.getId().equals(task.getId()))
					return -1;
				if (b.getId().equals(task.getId()))
					return 1;
				return a.getOrderIndex().compareTo(b.getOrderIndex());
			});

			IntStream.range(0, tasks.size()).forEach(i -> tasks.get(i).setOrderIndex(i));
			taskRepository.saveAll(tasks);

			return ApiResponse.success("Task relocated", null);
		}
		finally
		{
			redisLockUtil.unlock(lockKey, lockToken);
		}
	}

	@Override
	@Transactional
	public ApiResponse<Void> adjustColumnSequence(List<Long> columnIds)
	{
		IntStream.range(0, columnIds.size()).forEach(i ->
		{
			columnRepository.findById(columnIds.get(i)).ifPresent(column ->
			{
				column.setOrderIndex(i);
				columnRepository.save(column);
			});
		});
		return ApiResponse.success("Sequence adjusted", null);
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
