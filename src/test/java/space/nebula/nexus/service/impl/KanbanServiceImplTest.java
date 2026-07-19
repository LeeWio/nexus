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

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

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
	@InjectMocks
	private KanbanServiceImpl kanbanService;

	@Test
	@DisplayName("Should relocate task to the requested position and compact both columns")
	void relocateTask_Success() {
		// Arrange
		KanbanItemMoveRequest request = new KanbanItemMoveRequest();
		request.setItemId(1L);
		request.setTargetColumnId(2L);
		request.setTargetOrderIndex(0);

		KanbanItem task = new KanbanItem();
		task.setId(1L);
		task.setOrderIndex(1);
		KanbanItem sourceFirst = item(3L, 0);
		KanbanColumn sourceColumn = column(1L);
		task.setColumn(sourceColumn);
		sourceFirst.setColumn(sourceColumn);

		KanbanColumn destinationColumn = column(2L);
		KanbanItem destinationFirst = item(4L, 0);
		destinationFirst.setColumn(destinationColumn);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(columnRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(sourceColumn, destinationColumn));
		when(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(sourceFirst, task));
		when(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(2L)).thenReturn(List.of(destinationFirst));

		// Act
		kanbanService.relocateTask(request);

		// Assert
		assertEquals(destinationColumn, task.getColumn());
		assertEquals(0, task.getOrderIndex());
		assertEquals(1, destinationFirst.getOrderIndex());
		assertEquals(0, sourceFirst.getOrderIndex());
		verify(taskRepository, times(2)).saveAll(anyList());
	}

	@Test
	@DisplayName("Should reject a negative target position")
	void relocateTask_NegativePosition() {
		KanbanItemMoveRequest request = new KanbanItemMoveRequest();
		request.setItemId(1L);
		request.setTargetColumnId(2L);
		request.setTargetOrderIndex(-1);

		BusinessException exception = assertThrows(BusinessException.class, () -> kanbanService.relocateTask(request));
		assertTrue(exception.getMessage().contains("negative"));
		verify(taskRepository, never()).findByIdForUpdate(anyLong());
	}

	private static KanbanColumn column(Long id) {
		KanbanColumn column = new KanbanColumn();
		column.setId(id);
		column.setItems(new ArrayList<>());
		return column;
	}

	private static KanbanItem item(Long id, int orderIndex) {
		KanbanItem item = new KanbanItem();
		item.setId(id);
		item.setOrderIndex(orderIndex);
		return item;
	}
}
