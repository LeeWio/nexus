package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.entity.Tag;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanServiceImpl implements IKanbanService {

    private final KanbanColumnRepository columnRepository;
    private final KanbanItemRepository taskRepository;
    private final TagRepository tagRepository;
    private final KanbanMapper kanbanMapper;
    private final RedisLockUtil redisLockUtil;

    @Override
    @Transactional(readOnly = true)
    public List<KanbanColumnResponse> retrieveFullBoard() {
        List<KanbanColumn> boardColumns = columnRepository.findAllWithItemsOrderByOrderIndexAsc();
        return kanbanMapper.toColumnResponseList(boardColumns);
    }

    @Override
    @Transactional
    public KanbanColumnResponse createColumn(KanbanColumnRequest request) {
        KanbanColumn newColumn = new KanbanColumn();
        newColumn.setName(request.getName());
        newColumn.setColor(request.getColor());
        
        if (request.getOrderIndex() != null) {
            newColumn.setOrderIndex(request.getOrderIndex());
        } else {
            Integer maxSequence = columnRepository.findMaxOrderIndex();
            newColumn.setOrderIndex(maxSequence != null ? maxSequence + 1 : 0);
        }

        return kanbanMapper.toResponse(columnRepository.save(newColumn));
    }

    @Override
    @Transactional
    public KanbanColumnResponse updateColumn(Long id, KanbanColumnRequest request) {
        KanbanColumn columnToUpdate = columnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", id));
        
        columnToUpdate.setName(request.getName());
        columnToUpdate.setColor(request.getColor());
        if (request.getOrderIndex() != null) {
            columnToUpdate.setOrderIndex(request.getOrderIndex());
        }

        return kanbanMapper.toResponse(columnRepository.save(columnToUpdate));
    }

    @Override
    @Transactional
    public void deleteColumn(Long id) {
        if (!columnRepository.existsById(id)) {
            throw new ResourceNotFoundException("KanbanColumn", "id", id);
        }
        columnRepository.deleteById(id);
    }

    @Override
    @Transactional
    public KanbanItemResponse createTask(KanbanItemRequest request) {
        KanbanColumn parentColumn = columnRepository.findById(request.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", request.getColumnId()));

        KanbanItem newTask = new KanbanItem();
        newTask.setTitle(request.getTitle());
        newTask.setContent(request.getContent());
        newTask.setPriority(request.getPriority());
        newTask.setReminderAt(request.getReminderAt());
        newTask.setColumn(parentColumn);

        if (request.getOrderIndex() != null) {
            newTask.setOrderIndex(request.getOrderIndex());
        } else {
            Integer maxSequence = taskRepository.findMaxOrderIndexByColumnId(parentColumn.getId());
            newTask.setOrderIndex(maxSequence != null ? maxSequence + 1 : 0);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> associatedTags = tagRepository.findAllById(request.getTagIds());
            newTask.setTags(new HashSet<>(associatedTags));
        }

        return kanbanMapper.toResponse(taskRepository.save(newTask));
    }

    @Override
    @Transactional
    public KanbanItemResponse updateTask(Long id, KanbanItemRequest request) {
        KanbanItem taskToUpdate = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", id));

        kanbanMapper.updateItem(taskToUpdate, request);

        if (request.getColumnId() != null && !taskToUpdate.getColumn().getId().equals(request.getColumnId())) {
            KanbanColumn targetColumn = columnRepository.findById(request.getColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", request.getColumnId()));
            taskToUpdate.setColumn(targetColumn);
        }

        if (request.getTagIds() != null) {
            List<Tag> updatedTags = tagRepository.findAllById(request.getTagIds());
            taskToUpdate.setTags(new HashSet<>(updatedTags));
        }

        return kanbanMapper.toResponse(taskRepository.save(taskToUpdate));
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("KanbanItem", "id", id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void relocateTask(KanbanItemMoveRequest request) {
        String columnMutexKey = "nexus:lock:kanban:column:" + request.getTargetColumnId();
        String mutexToken = UUID.randomUUID().toString();

        if (!redisLockUtil.tryLock(columnMutexKey, mutexToken, 5, TimeUnit.SECONDS)) {
            throw new space.nebula.nexus.common.exception.BusinessException("Board is currently busy, please try again in a moment");
        }

        try {
            KanbanItem targetTask = taskRepository.findById(request.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("KanbanItem", "id", request.getItemId()));

            KanbanColumn destinationColumn = columnRepository.findById(request.getTargetColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("KanbanColumn", "id", request.getTargetColumnId()));

            targetTask.setColumn(destinationColumn);
            targetTask.setOrderIndex(request.getTargetOrderIndex());
            taskRepository.save(targetTask);

            List<KanbanItem> destinationColumnTasks = destinationColumn.getItems();
            if (!destinationColumnTasks.contains(targetTask)) {
                destinationColumnTasks.add(targetTask);
            }

            destinationColumnTasks.sort((a, b) -> {
                if (a.getId().equals(targetTask.getId())) return -1;
                if (b.getId().equals(targetTask.getId())) return 1;
                return a.getOrderIndex().compareTo(b.getOrderIndex());
            });

            for (int i = 0; i < destinationColumnTasks.size(); i++) {
                destinationColumnTasks.get(i).setOrderIndex(i);
            }
            taskRepository.saveAll(destinationColumnTasks);
        } finally {
            redisLockUtil.unlock(columnMutexKey, mutexToken);
        }
    }

    @Override
    @Transactional
    public void adjustColumnSequence(List<Long> columnIds) {
        IntStream.range(0, columnIds.size()).forEach(i -> {
            columnRepository.findById(columnIds.get(i)).ifPresent(column -> {
                column.setOrderIndex(i);
                columnRepository.save(column);
            });
        });
    }
}
