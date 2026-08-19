package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanChecklistItemCompletionRequest;
import space.nebula.nexus.payload.request.KanbanChecklistItemRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.request.KanbanTaskAssigneeRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanChecklistItemResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;
import space.nebula.nexus.service.IKanbanService;

import java.util.List;

/**
 * Controller for administrative Kanban board management. Provides endpoints for
 * columns, tasks, and board organization.
 */
@Tag(name = "Admin Kanban", description = "Kanban board management APIs for administrators")
@RestController
@RequestMapping("/api/v1/admin/kanban")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminKanbanController {

	private final IKanbanService kanbanService;

	@GetMapping
	@Operation(summary = "Retrieve Kanban board", description = "Fetch the full board state including all columns and items.")
	public ApiResponse<List<KanbanColumnResponse>> retrieveBoard() {
		return kanbanService.retrieveFullBoard();
	}

	@PostMapping("/columns")
	@Operation(summary = "Create column", description = "Add a new column to the Kanban board.")
	public ApiResponse<KanbanColumnResponse> createColumn(@Valid @RequestBody KanbanColumnRequest request) {
		return kanbanService.createColumn(request);
	}

	@PutMapping("/columns/{id}")
	@Operation(summary = "Update column", description = "Modify a column's name or color. Use the sequence endpoint for ordering.")
	public ApiResponse<KanbanColumnResponse> updateColumn(@Parameter(description = "Column ID") @PathVariable Long id,
			@Valid @RequestBody KanbanColumnRequest request) {
		return kanbanService.updateColumn(id, request);
	}

	@DeleteMapping("/columns/{id}")
	@Operation(summary = "Delete column", description = "Remove an empty column. Tasks must be moved or deleted first.")
	public ApiResponse<Void> deleteColumn(@Parameter(description = "Column ID") @PathVariable Long id) {
		return kanbanService.deleteColumn(id);
	}

	@PostMapping("/tasks")
	@Operation(summary = "Create task", description = "Add a new task item to a specific column.")
	public ApiResponse<KanbanItemResponse> createTask(@Valid @RequestBody KanbanItemRequest request) {
		return kanbanService.createTask(request);
	}

	@PutMapping("/tasks/{id}")
	@Operation(summary = "Update task", description = "Modify task details, priority, or tags.")
	public ApiResponse<KanbanItemResponse> updateTask(@Parameter(description = "Task ID") @PathVariable Long id,
			@Valid @RequestBody KanbanItemRequest request) {
		return kanbanService.updateTask(id, request);
	}

	@GetMapping("/tasks/{taskId}/checklist")
	@Operation(summary = "Retrieve task checklist", description = "Fetch the ordered checklist items for a task.")
	public ApiResponse<List<KanbanChecklistItemResponse>> retrieveChecklistItems(
			@Parameter(description = "Task ID") @PathVariable Long taskId) {
		return kanbanService.retrieveChecklistItems(taskId);
	}

	@PostMapping("/tasks/{taskId}/checklist")
	@Operation(summary = "Create checklist item", description = "Add an ordered checklist item to a task.")
	public ApiResponse<KanbanChecklistItemResponse> createChecklistItem(
			@Parameter(description = "Task ID") @PathVariable Long taskId,
			@Valid @RequestBody KanbanChecklistItemRequest request) {
		return kanbanService.createChecklistItem(taskId, request);
	}

	@PutMapping("/tasks/{taskId}/checklist/{checklistItemId}")
	@Operation(summary = "Update checklist item", description = "Modify checklist text or completion state.")
	public ApiResponse<KanbanChecklistItemResponse> updateChecklistItem(
			@Parameter(description = "Task ID") @PathVariable Long taskId,
			@Parameter(description = "Checklist item ID") @PathVariable Long checklistItemId,
			@Valid @RequestBody KanbanChecklistItemRequest request) {
		return kanbanService.updateChecklistItem(taskId, checklistItemId, request);
	}

	@PatchMapping("/tasks/{taskId}/checklist/{checklistItemId}/completion")
	@Operation(summary = "Set checklist completion", description = "Mark one checklist item complete or incomplete.")
	public ApiResponse<KanbanChecklistItemResponse> completeChecklistItem(
			@Parameter(description = "Task ID") @PathVariable Long taskId,
			@Parameter(description = "Checklist item ID") @PathVariable Long checklistItemId,
			@Valid @RequestBody KanbanChecklistItemCompletionRequest request) {
		return kanbanService.completeChecklistItem(taskId, checklistItemId, request);
	}

	@DeleteMapping("/tasks/{taskId}/checklist/{checklistItemId}")
	@Operation(summary = "Delete checklist item", description = "Remove one checklist item and compact the remaining order.")
	public ApiResponse<Void> deleteChecklistItem(@Parameter(description = "Task ID") @PathVariable Long taskId,
			@Parameter(description = "Checklist item ID") @PathVariable Long checklistItemId) {
		return kanbanService.deleteChecklistItem(taskId, checklistItemId);
	}

	@PostMapping("/tasks/{taskId}/checklist/sequence")
	@Operation(summary = "Adjust checklist sequence", description = "Reorder every checklist item on a task.")
	public ApiResponse<Void> adjustChecklistItemSequence(@Parameter(description = "Task ID") @PathVariable Long taskId,
			@RequestBody List<Long> checklistItemIds) {
		return kanbanService.adjustChecklistItemSequence(taskId, checklistItemIds);
	}

	@PutMapping("/tasks/{id}/assignees")
	@Operation(summary = "Assign task users", description = "Replace the active users assigned to a task.")
	public ApiResponse<KanbanItemResponse> assignTaskAssignees(
			@Parameter(description = "Task ID") @PathVariable Long id,
			@Valid @RequestBody KanbanTaskAssigneeRequest request) {
		return kanbanService.assignTaskAssignees(id, request);
	}

	@PostMapping("/tasks/{id}/duplicate")
	@Operation(summary = "Duplicate task", description = "Create a copy immediately after the source task in the same column.")
	public ApiResponse<KanbanItemResponse> duplicateTask(@Parameter(description = "Task ID") @PathVariable Long id) {
		return kanbanService.duplicateTask(id);
	}

	@DeleteMapping("/tasks/{id}")
	@Operation(summary = "Delete task", description = "Permanently remove a task item.")
	public ApiResponse<Void> deleteTask(@Parameter(description = "Task ID") @PathVariable Long id) {
		return kanbanService.deleteTask(id);
	}

	@PostMapping("/tasks/relocate")
	@Operation(summary = "Relocate task", description = "Move a task to another column or change its position.")
	public ApiResponse<Void> relocateTask(@Valid @RequestBody KanbanItemMoveRequest request) {
		return kanbanService.relocateTask(request);
	}

	@PostMapping("/columns/sequence")
	@Operation(summary = "Adjust column sequence", description = "Reorder the horizontal positions of columns.")
	public ApiResponse<Void> adjustColumnSequence(@RequestBody List<Long> columnIds) {
		return kanbanService.adjustColumnSequence(columnIds);
	}
}
