package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.INotificationService;

@Tag(name = "Notification API", description = "Endpoints for managing user notifications")
@RestController
@RequestMapping("/api/v1/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications", description = "Retrieve a paginated notification inbox, optionally filtered to unread items.")
    public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getMyNotifications(unreadOnly, pageable);
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a notification owned by the current user.")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        return notificationService.deleteNotification(id);
    }

    @DeleteMapping("/read")
    @Operation(summary = "Clear read notifications", description = "Delete all read notifications owned by the current user.")
    public ApiResponse<Void> clearReadNotifications() {
        return notificationService.clearReadNotifications();
    }
}
