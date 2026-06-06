package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.INotificationService;

@Tag(name = "Notification API", description = "Endpoints for managing user notifications")
@RestController
@RequestMapping("/api/v1/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications", description = "Retrieve a paginated list of notifications for the current user.")
    public ApiResponse<PageResult<Notification>> getMyNotifications(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getMyNotifications(pageable);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread count", description = "Retrieve the number of unread notifications.")
    public ApiResponse<Long> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a specific notification as read.")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications for the current user as read.")
    public ApiResponse<Void> markAllAsRead() {
        return notificationService.markAllAsRead();
    }
}
