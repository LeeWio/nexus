package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.KanbanColumn;
import space.nebula.nexus.entity.KanbanChecklistItem;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.KanbanPriority;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.mapper.KanbanMapper;
import space.nebula.nexus.payload.request.KanbanChecklistItemCompletionRequest;
import space.nebula.nexus.payload.request.KanbanChecklistItemRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.request.KanbanTaskAssigneeRequest;
import space.nebula.nexus.repository.KanbanColumnRepository;
import space.nebula.nexus.repository.KanbanChecklistItemRepository;
import space.nebula.nexus.repository.KanbanItemRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Set;

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
	private KanbanChecklistItemRepository checklistItemRepository;
	@Mock
	private TagRepository tagRepository;
	@Mock
	private UserRepository userRepository;
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
		when(columnRepository.findAllByIdForUpdate(anyCollection()))
				.thenReturn(List.of(sourceColumn, destinationColumn));
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

	@Test
	@DisplayName("Should duplicate a task immediately after its source and renumber the column")
	void duplicateTask_CopiesTaskAndKeepsColumnOrderContinuous() {
		KanbanColumn column = column(1L);
		KanbanItem first = item(1L, 0);
		KanbanItem source = item(2L, 1);
		KanbanItem following = item(3L, 2);
		Tag tag = new Tag();
		tag.setId(8L);
		source.setTitle("Prepare release notes");
		source.setContent("Include migration notes");
		source.setColumn(column);
		source.setReminderAt(java.time.LocalDateTime.of(2026, 8, 10, 9, 0));
		source.setTags(new java.util.HashSet<>(Set.of(tag)));
		User assignee = activeUser(9L);
		source.setAssignees(new java.util.HashSet<>(Set.of(assignee)));
		KanbanChecklistItem sourceChecklistItem = checklistItem(10L, 0);
		sourceChecklistItem.setTitle("Confirm release owner");
		sourceChecklistItem.setCompleted(true);
		sourceChecklistItem.setTask(source);
		source.setChecklistItems(new ArrayList<>(List.of(sourceChecklistItem)));
		first.setColumn(column);
		following.setColumn(column);

		when(taskRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(source));
		when(columnRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(column));
		when(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(1L))
				.thenReturn(List.of(first, source, following));

		kanbanService.duplicateTask(2L);

		ArgumentCaptor<List<KanbanItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(taskRepository).saveAll(itemsCaptor.capture());
		List<KanbanItem> savedItems = itemsCaptor.getValue();
		KanbanItem duplicate = savedItems.get(2);

		assertEquals(4, savedItems.size());
		assertEquals("Copy of Prepare release notes", duplicate.getTitle());
		assertEquals(source.getContent(), duplicate.getContent());
		assertEquals(source.getReminderAt(), duplicate.getReminderAt());
		assertEquals(column, duplicate.getColumn());
		assertEquals(source.getTags(), duplicate.getTags());
		assertNotSame(source.getTags(), duplicate.getTags());
		assertEquals(source.getAssignees(), duplicate.getAssignees());
		assertNotSame(source.getAssignees(), duplicate.getAssignees());
		assertEquals(1, duplicate.getChecklistItems().size());
		assertNotSame(source.getChecklistItems(), duplicate.getChecklistItems());
		assertEquals("Confirm release owner", duplicate.getChecklistItems().getFirst().getTitle());
		assertTrue(duplicate.getChecklistItems().getFirst().getCompleted());
		assertSame(duplicate, duplicate.getChecklistItems().getFirst().getTask());
		assertEquals(0, first.getOrderIndex());
		assertEquals(1, source.getOrderIndex());
		assertEquals(2, duplicate.getOrderIndex());
		assertEquals(3, following.getOrderIndex());
	}

	@Test
	@DisplayName("Should assign active users when creating a task")
	void createTask_AssignsActiveUsers() {
		KanbanColumn column = column(1L);
		User assignee = activeUser(8L);
		KanbanItemRequest request = new KanbanItemRequest();
		request.setTitle("Review release notes");
		request.setPriority(KanbanPriority.MEDIUM);
		request.setColumnId(1L);
		request.setAssigneeIds(Set.of(8L));

		when(columnRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(column));
		when(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of());
		when(userRepository.findAllById(Set.of(8L))).thenReturn(List.of(assignee));
		when(taskRepository.save(any(KanbanItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		kanbanService.createTask(request);

		ArgumentCaptor<KanbanItem> taskCaptor = ArgumentCaptor.forClass(KanbanItem.class);
		verify(taskRepository).save(taskCaptor.capture());
		assertEquals(Set.of(assignee), taskCaptor.getValue().getAssignees());
	}

	@Test
	@DisplayName("Should replace task assignees with active users")
	void assignTaskAssignees_ReplacesAssignees() {
		KanbanItem task = item(1L, 0);
		User assignee = activeUser(8L);
		KanbanTaskAssigneeRequest request = new KanbanTaskAssigneeRequest(Set.of(8L));

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(userRepository.findAllById(Set.of(8L))).thenReturn(List.of(assignee));
		when(taskRepository.save(task)).thenReturn(task);

		kanbanService.assignTaskAssignees(1L, request);

		assertEquals(Set.of(assignee), task.getAssignees());
		verify(taskRepository).save(task);
	}

	@Test
	@DisplayName("Should clear all task assignees without loading users")
	void assignTaskAssignees_ClearsAssignees() {
		KanbanItem task = item(1L, 0);
		task.setAssignees(new java.util.HashSet<>(Set.of(activeUser(8L))));
		KanbanTaskAssigneeRequest request = new KanbanTaskAssigneeRequest(Set.of());

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(taskRepository.save(task)).thenReturn(task);

		kanbanService.assignTaskAssignees(1L, request);

		assertTrue(task.getAssignees().isEmpty());
		verify(userRepository, never()).findAllById(anyCollection());
		verify(taskRepository).save(task);
	}

	@Test
	@DisplayName("Should reject nonexistent task assignees")
	void assignTaskAssignees_RejectsMissingUsers() {
		KanbanItem task = item(1L, 0);
		KanbanTaskAssigneeRequest request = new KanbanTaskAssigneeRequest(Set.of(8L));

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(userRepository.findAllById(Set.of(8L))).thenReturn(List.of());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> kanbanService.assignTaskAssignees(1L, request));

		assertTrue(exception.getMessage().contains("do not exist"));
		verify(taskRepository, never()).save(any(KanbanItem.class));
	}

	@Test
	@DisplayName("Should reject null task assignee IDs")
	void assignTaskAssignees_RejectsNullUserIds() {
		KanbanItem task = item(1L, 0);
		Set<Long> assigneeIds = new java.util.HashSet<>();
		assigneeIds.add(null);
		KanbanTaskAssigneeRequest request = new KanbanTaskAssigneeRequest(assigneeIds);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> kanbanService.assignTaskAssignees(1L, request));

		assertTrue(exception.getMessage().contains("null values"));
		verify(userRepository, never()).findAllById(anyCollection());
		verify(taskRepository, never()).save(any(KanbanItem.class));
	}

	@Test
	@DisplayName("Should reject inactive users as task assignees")
	void assignTaskAssignees_RejectsInactiveUsers() {
		KanbanItem task = item(1L, 0);
		User inactiveUser = activeUser(8L);
		inactiveUser.setStatus(UserStatus.INACTIVE);
		KanbanTaskAssigneeRequest request = new KanbanTaskAssigneeRequest(Set.of(8L));

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(userRepository.findAllById(Set.of(8L))).thenReturn(List.of(inactiveUser));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> kanbanService.assignTaskAssignees(1L, request));

		assertTrue(exception.getMessage().contains("active accounts"));
		verify(taskRepository, never()).save(any(KanbanItem.class));
	}

	@Test
	@DisplayName("Should soft-delete checklist items when deleting their task")
	void deleteTask_DeletesChecklistItems() {
		KanbanColumn column = column(1L);
		KanbanItem task = item(2L, 0);
		task.setColumn(column);
		KanbanChecklistItem checklistItem = checklistItem(11L, 0);
		checklistItem.setTask(task);

		when(taskRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(task));
		when(columnRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(column));
		when(taskRepository.findByColumnIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(2L)).thenReturn(List.of(checklistItem));

		kanbanService.deleteTask(2L);

		verify(checklistItemRepository).deleteAll(List.of(checklistItem));
		verify(taskRepository).delete(task);
	}

	@Test
	@DisplayName("Should insert a checklist item at the requested position and renumber the task")
	void createChecklistItem_InsertsAndRenumbers() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem first = checklistItem(11L, 0);
		KanbanChecklistItem following = checklistItem(12L, 1);
		KanbanChecklistItemRequest request = new KanbanChecklistItemRequest("Confirm copy", false, 1);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(first, following));

		kanbanService.createChecklistItem(1L, request);

		ArgumentCaptor<List<KanbanChecklistItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(checklistItemRepository).saveAll(itemsCaptor.capture());
		List<KanbanChecklistItem> savedItems = itemsCaptor.getValue();
		KanbanChecklistItem inserted = savedItems.get(1);

		assertEquals(3, savedItems.size());
		assertEquals("Confirm copy", inserted.getTitle());
		assertFalse(inserted.getCompleted());
		assertSame(task, inserted.getTask());
		assertEquals(0, first.getOrderIndex());
		assertEquals(1, inserted.getOrderIndex());
		assertEquals(2, following.getOrderIndex());
	}

	@Test
	@DisplayName("Should update checklist completion without changing its position")
	void completeChecklistItem_UpdatesState() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem checklistItem = checklistItem(11L, 0);
		checklistItem.setCompleted(false);
		KanbanChecklistItemCompletionRequest request = new KanbanChecklistItemCompletionRequest(true);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(checklistItem));
		when(checklistItemRepository.save(checklistItem)).thenReturn(checklistItem);

		kanbanService.completeChecklistItem(1L, 11L, request);

		assertTrue(checklistItem.getCompleted());
		assertEquals(0, checklistItem.getOrderIndex());
		verify(checklistItemRepository).save(checklistItem);
	}

	@Test
	@DisplayName("Should move an updated checklist item and renumber every item")
	void updateChecklistItem_MovesAndRenumbers() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem first = checklistItem(11L, 0);
		KanbanChecklistItem second = checklistItem(12L, 1);
		KanbanChecklistItemRequest request = new KanbanChecklistItemRequest("Refine release notes", true, 0);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(first, second));

		kanbanService.updateChecklistItem(1L, 12L, request);

		assertEquals("Refine release notes", second.getTitle());
		assertTrue(second.getCompleted());
		assertEquals(0, second.getOrderIndex());
		assertEquals(1, first.getOrderIndex());
		verify(checklistItemRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("Should delete a checklist item and compact the remaining order")
	void deleteChecklistItem_CompactsOrder() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem first = checklistItem(11L, 0);
		KanbanChecklistItem removed = checklistItem(12L, 1);
		KanbanChecklistItem following = checklistItem(13L, 2);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L))
				.thenReturn(List.of(first, removed, following));

		kanbanService.deleteChecklistItem(1L, 12L);

		verify(checklistItemRepository).delete(removed);
		assertEquals(0, first.getOrderIndex());
		assertEquals(1, following.getOrderIndex());
		ArgumentCaptor<List<KanbanChecklistItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(checklistItemRepository).saveAll(itemsCaptor.capture());
		assertEquals(List.of(first, following), itemsCaptor.getValue());
	}

	@Test
	@DisplayName("Should apply a complete checklist sequence")
	void adjustChecklistItemSequence_ReordersAllItems() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem first = checklistItem(11L, 0);
		KanbanChecklistItem second = checklistItem(12L, 1);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(first, second));

		kanbanService.adjustChecklistItemSequence(1L, List.of(12L, 11L));

		assertEquals(0, second.getOrderIndex());
		assertEquals(1, first.getOrderIndex());
		verify(checklistItemRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("Should reject a checklist sequence that omits an item")
	void adjustChecklistItemSequence_RejectsIncompleteSequence() {
		KanbanItem task = item(1L, 0);
		KanbanChecklistItem first = checklistItem(11L, 0);
		KanbanChecklistItem second = checklistItem(12L, 1);

		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));
		when(checklistItemRepository.findByTaskIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(first, second));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> kanbanService.adjustChecklistItemSequence(1L, List.of(11L)));

		assertTrue(exception.getMessage().contains("every task checklist item"));
		verify(checklistItemRepository, never()).saveAll(anyList());
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

	private static KanbanChecklistItem checklistItem(Long id, int orderIndex) {
		KanbanChecklistItem checklistItem = new KanbanChecklistItem();
		checklistItem.setId(id);
		checklistItem.setOrderIndex(orderIndex);
		return checklistItem;
	}

	private static User activeUser(Long id) {
		User user = new User();
		user.setId(id);
		user.setUsername("user-" + id);
		user.setStatus(UserStatus.ACTIVE);
		return user;
	}
}
