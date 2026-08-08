package space.nebula.nexus.service;

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

import java.util.List;

/**
 * Service interface for Kanban board management. Provides APIs for handling
 * columns and tasks with structured responses.
 */
public interface IKanbanService {

	/**
	 * Retrieves the entire board including all columns and tasks.
	 *
	 * @return ApiResponse containing the list of Kanban columns with their items.
	 */
	ApiResponse<List<KanbanColumnResponse>> retrieveFullBoard();

	/**
	 * Creates a new Kanban column.
	 *
	 * @param request
	 *            the column creation data
	 * @return ApiResponse containing the created column details.
	 */
	ApiResponse<KanbanColumnResponse> createColumn(KanbanColumnRequest request);

	/**
	 * Updates an existing Kanban column.
	 *
	 * @param id
	 *            the column ID
	 * @param request
	 *            the column update data
	 * @return ApiResponse containing the updated column details.
	 */
	ApiResponse<KanbanColumnResponse> updateColumn(Long id, KanbanColumnRequest request);

	/**
	 * Deletes a Kanban column and its associated tasks.
	 *
	 * @param id
	 *            the column ID
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> deleteColumn(Long id);

	/**
	 * Creates a new Kanban task (item).
	 *
	 * @param request
	 *            the task creation data
	 * @return ApiResponse containing the created task details.
	 */
	ApiResponse<KanbanItemResponse> createTask(KanbanItemRequest request);

	/**
	 * Updates an existing Kanban task.
	 *
	 * @param id
	 *            the task ID
	 * @param request
	 *            the task update data
	 * @return ApiResponse containing the updated task details.
	 */
	ApiResponse<KanbanItemResponse> updateTask(Long id, KanbanItemRequest request);

	/**
	 * Duplicates a task immediately after its source task in the same column.
	 *
	 * @param id
	 *            the task to duplicate
	 * @return ApiResponse containing the duplicated task details.
	 */
	ApiResponse<KanbanItemResponse> duplicateTask(Long id);

	/**
	 * Replaces the active users assigned to a Kanban task.
	 *
	 * @param id
	 *            the task identifier
	 * @param request
	 *            the complete set of assigned user IDs
	 * @return ApiResponse containing the updated task details.
	 */
	ApiResponse<KanbanItemResponse> assignTaskAssignees(Long id, KanbanTaskAssigneeRequest request);

	/**
	 * Retrieves the ordered checklist for a Kanban task.
	 *
	 * @param taskId
	 *            the task identifier
	 * @return ApiResponse containing the task checklist.
	 */
	ApiResponse<List<KanbanChecklistItemResponse>> retrieveChecklistItems(Long taskId);

	/**
	 * Adds a checklist item to a Kanban task.
	 *
	 * @param taskId
	 *            the task identifier
	 * @param request
	 *            the checklist item data
	 * @return ApiResponse containing the created checklist item.
	 */
	ApiResponse<KanbanChecklistItemResponse> createChecklistItem(Long taskId, KanbanChecklistItemRequest request);

	/**
	 * Updates a checklist item's text and completion state.
	 *
	 * @param taskId
	 *            the task identifier
	 * @param checklistItemId
	 *            the checklist item identifier
	 * @param request
	 *            the updated checklist item data
	 * @return ApiResponse containing the updated checklist item.
	 */
	ApiResponse<KanbanChecklistItemResponse> updateChecklistItem(Long taskId, Long checklistItemId,
			KanbanChecklistItemRequest request);

	/**
	 * Changes the completion state of one checklist item.
	 *
	 * @param taskId
	 *            the task identifier
	 * @param checklistItemId
	 *            the checklist item identifier
	 * @param request
	 *            the requested completion state
	 * @return ApiResponse containing the updated checklist item.
	 */
	ApiResponse<KanbanChecklistItemResponse> completeChecklistItem(Long taskId, Long checklistItemId,
			KanbanChecklistItemCompletionRequest request);

	/**
	 * Removes a checklist item and compacts the remaining order.
	 *
	 * @param taskId
	 *            the task identifier
	 * @param checklistItemId
	 *            the checklist item identifier
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> deleteChecklistItem(Long taskId, Long checklistItemId);

	/**
	 * Replaces the ordering of every checklist item on a task.
	 *
	 * @param taskId
	 *            the task identifier
	 * @param checklistItemIds
	 *            every checklist item ID in its intended order
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> adjustChecklistItemSequence(Long taskId, List<Long> checklistItemIds);

	/**
	 * Deletes a Kanban task.
	 *
	 * @param id
	 *            the task ID
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> deleteTask(Long id);

	/**
	 * Relocates a task to a different column or a different position within a
	 * column.
	 *
	 * @param request
	 *            the move request details
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> relocateTask(KanbanItemMoveRequest request);

	/**
	 * Adjusts the horizontal sequence of columns.
	 *
	 * @param columnIds
	 *            the ordered list of column IDs
	 * @return ApiResponse indicating success or failure.
	 */
	ApiResponse<Void> adjustColumnSequence(List<Long> columnIds);
}
