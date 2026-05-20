package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;

import java.util.List;

/**
 * Service interface for Kanban board management.
 * Provides APIs for handling columns and tasks with structured responses.
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
     * @param request the column creation data
     * @return ApiResponse containing the created column details.
     */
    ApiResponse<KanbanColumnResponse> createColumn(KanbanColumnRequest request);

    /**
     * Updates an existing Kanban column.
     *
     * @param id the column ID
     * @param request the column update data
     * @return ApiResponse containing the updated column details.
     */
    ApiResponse<KanbanColumnResponse> updateColumn(Long id, KanbanColumnRequest request);

    /**
     * Deletes a Kanban column and its associated tasks.
     *
     * @param id the column ID
     * @return ApiResponse indicating success or failure.
     */
    ApiResponse<Void> deleteColumn(Long id);

    /**
     * Creates a new Kanban task (item).
     *
     * @param request the task creation data
     * @return ApiResponse containing the created task details.
     */
    ApiResponse<KanbanItemResponse> createTask(KanbanItemRequest request);

    /**
     * Updates an existing Kanban task.
     *
     * @param id the task ID
     * @param request the task update data
     * @return ApiResponse containing the updated task details.
     */
    ApiResponse<KanbanItemResponse> updateTask(Long id, KanbanItemRequest request);

    /**
     * Deletes a Kanban task.
     *
     * @param id the task ID
     * @return ApiResponse indicating success or failure.
     */
    ApiResponse<Void> deleteTask(Long id);

    /**
     * Relocates a task to a different column or a different position within a column.
     *
     * @param request the move request details
     * @return ApiResponse indicating success or failure.
     */
    ApiResponse<Void> relocateTask(KanbanItemMoveRequest request);

    /**
     * Adjusts the horizontal sequence of columns.
     *
     * @param columnIds the ordered list of column IDs
     * @return ApiResponse indicating success or failure.
     */
    ApiResponse<Void> adjustColumnSequence(List<Long> columnIds);
}
