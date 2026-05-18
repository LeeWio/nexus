package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.KanbanColumnRequest;
import space.nebula.nexus.payload.request.KanbanItemMoveRequest;
import space.nebula.nexus.payload.request.KanbanItemRequest;
import space.nebula.nexus.payload.response.KanbanColumnResponse;
import space.nebula.nexus.payload.response.KanbanItemResponse;
import space.nebula.nexus.service.IKanbanService;

import java.util.List;

@Tag(name = "Admin Kanban", description = "Kanban board management APIs for admin")
@RestController
@RequestMapping("/api/admin/kanban")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminKanbanController {

    private final IKanbanService kanbanService;

    @Operation(summary = "Retrieve the full Kanban board state")
    @GetMapping
    public ApiResponse<List<KanbanColumnResponse>> retrieveBoard() {
        return ApiResponse.success(kanbanService.retrieveFullBoard());
    }

    @Operation(summary = "Create a new Kanban column")
    @PostMapping("/columns")
    public ApiResponse<KanbanColumnResponse> createColumn(@Valid @RequestBody KanbanColumnRequest request) {
        return ApiResponse.success(kanbanService.createColumn(request));
    }

    @Operation(summary = "Update a Kanban column")
    @PutMapping("/columns/{id}")
    public ApiResponse<KanbanColumnResponse> updateColumn(@PathVariable Long id, @Valid @RequestBody KanbanColumnRequest request) {
        return ApiResponse.success(kanbanService.updateColumn(id, request));
    }

    @Operation(summary = "Delete a Kanban column")
    @DeleteMapping("/columns/{id}")
    public ApiResponse<Void> deleteColumn(@PathVariable Long id) {
        kanbanService.deleteColumn(id);
        return ApiResponse.success();
    }

    @Operation(summary = "Create a new task")
    @PostMapping("/tasks")
    public ApiResponse<KanbanItemResponse> createTask(@Valid @RequestBody KanbanItemRequest request) {
        return ApiResponse.success(kanbanService.createTask(request));
    }

    @Operation(summary = "Update a task")
    @PutMapping("/tasks/{id}")
    public ApiResponse<KanbanItemResponse> updateTask(@PathVariable Long id, @Valid @RequestBody KanbanItemRequest request) {
        return ApiResponse.success(kanbanService.updateTask(id, request));
    }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        kanbanService.deleteTask(id);
        return ApiResponse.success();
    }

    @Operation(summary = "Relocate a task between columns or adjust position")
    @PostMapping("/tasks/relocate")
    public ApiResponse<Void> relocateTask(@Valid @RequestBody KanbanItemMoveRequest request) {
        kanbanService.relocateTask(request);
        return ApiResponse.success();
    }

    @Operation(summary = "Adjust column display sequence")
    @PostMapping("/columns/sequence")
    public ApiResponse<Void> adjustColumnSequence(@RequestBody List<Long> columnIds) {
        kanbanService.adjustColumnSequence(columnIds);
        return ApiResponse.success();
    }
}
