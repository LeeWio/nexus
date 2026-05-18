package space.nebula.nexus.service;

import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;

import java.util.List;

public interface IKanbanService {

    /**
     * Retrieves the entire board including all columns and tasks.
     */
    List<KanbanColumnResponse> retrieveFullBoard();

    KanbanColumnResponse createColumn(KanbanColumnRequest request);

    KanbanColumnResponse updateColumn(Long id, KanbanColumnRequest request);

    void deleteColumn(Long id);

    KanbanItemResponse createTask(KanbanItemRequest request);

    KanbanItemResponse updateTask(Long id, KanbanItemRequest request);

    void deleteTask(Long id);

    /**
     * Relocates a task to a different column or a different position.
     */
    void relocateTask(KanbanItemMoveRequest request);

    /**
     * Adjusts the horizontal sequence of columns.
     */
    void adjustColumnSequence(List<Long> columnIds);
}
