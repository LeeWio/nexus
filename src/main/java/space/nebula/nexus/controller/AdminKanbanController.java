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
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
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
public class AdminKanbanController
{

	private final IKanbanService kanbanService;

	@GetMapping
	@Operation(summary = "Retrieve Kanban board", description = "Fetch the full board state including all columns and items.")
	public ApiResponse<List<KanbanColumnResponse>> retrieveBoard()
	{
		return kanbanService.retrieveFullBoard();
	}

	@PostMapping("/columns")
	@Operation(summary = "Create column", description = "Add a new column to the Kanban board.")
	public ApiResponse<KanbanColumnResponse> createColumn(@Valid @RequestBody KanbanColumnRequest request)
	{
		return kanbanService.createColumn(request);
	}

	@PutMapping("/columns/{id}")
	@Operation(summary = "Update column", description = "Modify the name, color, or order of an existing column.")
	public ApiResponse<KanbanColumnResponse> updateColumn(@Parameter(description = "Column ID") @PathVariable Long id,
			@Valid @RequestBody KanbanColumnRequest request)
	{
		return kanbanService.updateColumn(id, request);
	}

	@DeleteMapping("/columns/{id}")
	@Operation(summary = "Delete column", description = "Remove a column and all its associated tasks.")
	public ApiResponse<Void> deleteColumn(@Parameter(description = "Column ID") @PathVariable Long id)
	{
		return kanbanService.deleteColumn(id);
	}

	@PostMapping("/tasks")
	@Operation(summary = "Create task", description = "Add a new task item to a specific column.")
	public ApiResponse<KanbanItemResponse> createTask(@Valid @RequestBody KanbanItemRequest request)
	{
		return kanbanService.createTask(request);
	}

	@PutMapping("/tasks/{id}")
	@Operation(summary = "Update task", description = "Modify task details, priority, or tags.")
	public ApiResponse<KanbanItemResponse> updateTask(@Parameter(description = "Task ID") @PathVariable Long id,
			@Valid @RequestBody KanbanItemRequest request)
	{
		return kanbanService.updateTask(id, request);
	}

	@DeleteMapping("/tasks/{id}")
	@Operation(summary = "Delete task", description = "Permanently remove a task item.")
	public ApiResponse<Void> deleteTask(@Parameter(description = "Task ID") @PathVariable Long id)
	{
		return kanbanService.deleteTask(id);
	}

	@PostMapping("/tasks/relocate")
	@Operation(summary = "Relocate task", description = "Move a task to another column or change its position.")
	public ApiResponse<Void> relocateTask(@Valid @RequestBody KanbanItemMoveRequest request)
	{
		return kanbanService.relocateTask(request);
	}

	@PostMapping("/columns/sequence")
	@Operation(summary = "Adjust column sequence", description = "Reorder the horizontal positions of columns.")
	public ApiResponse<Void> adjustColumnSequence(@RequestBody List<Long> columnIds)
	{
		return kanbanService.adjustColumnSequence(columnIds);
	}
}
