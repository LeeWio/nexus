package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.mapper.KanbanMapper;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.repository.KanbanColumnRepository;
import space.nebula.nexus.repository.KanbanItemRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.utils.RedisLockUtil;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KanbanServiceImplTest {

	@Mock
	private KanbanColumnRepository columnRepository;
	@Mock
	private KanbanItemRepository taskRepository;
	@Mock
	private TagRepository tagRepository;
	@Mock
	private KanbanMapper kanbanMapper;
	@Mock
	private RedisLockUtil redisLockUtil;

	@InjectMocks
	private KanbanServiceImpl kanbanService;

	@Test
	@DisplayName("Should successfully relocate task and release mutex lock")
	void relocateTask_Success() {
		// Arrange
		KanbanItemMoveRequest request = new KanbanItemMoveRequest();
		request.setItemId(1L);
		request.setTargetColumnId(2L);
		request.setTargetOrderIndex(0);

		KanbanItem task = new KanbanItem();
		task.setId(1L);
		task.setOrderIndex(5);

		KanbanColumn destinationColumn = new KanbanColumn();
		destinationColumn.setId(2L);
		destinationColumn.setItems(new ArrayList<>());

		when(redisLockUtil.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
		when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
		when(columnRepository.findById(2L)).thenReturn(Optional.of(destinationColumn));

		// Act
		kanbanService.relocateTask(request);

		// Assert
		assertEquals(destinationColumn, task.getColumn());
		verify(taskRepository).saveAll(anyList());
		verify(redisLockUtil).unlock(anyString(), anyString());
	}

	@Test
	@DisplayName("Should fail to relocate task if column mutex is locked")
	void relocateTask_MutexLocked() {
		// Arrange
		KanbanItemMoveRequest request = new KanbanItemMoveRequest();
		request.setTargetColumnId(2L);
		when(redisLockUtil.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

		// Act & Assert
		BusinessException exception = assertThrows(BusinessException.class, () -> kanbanService.relocateTask(request));
		assertTrue(exception.getMessage().contains("busy"));
		verify(taskRepository, never()).findById(anyLong());
	}
}
