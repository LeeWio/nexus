package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.NotificationPreferenceRequest;
import space.nebula.nexus.payload.response.NotificationPreferenceResponse;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.INotificationService;

@Tag(name = "Notification API", description = "Endpoints for managing user notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final INotificationService notificationService;

	@GetMapping
	@Operation(summary = "Get my notifications", description = "Retrieve the current user's inbox. Use unreadOnly for badge and inbox filtering; newest notifications appear first by default.")
	public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(
			@Parameter(description = "When true, return only unread notifications", example = "false") @RequestParam(defaultValue = "false") boolean unreadOnly,
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return notificationService.getMyNotifications(unreadOnly, pageable);
	}

	@GetMapping("/unread/count")
	@Operation(summary = "Get unread count", description = "Retrieve the current user's unread inbox count for notification badges.")
	public ApiResponse<Long> getUnreadCount() {
		return notificationService.getUnreadCount();
	}

	@GetMapping("/preferences")
	@Operation(summary = "Get notification preferences", description = "Retrieve delivery settings for comment activity, followed-category posts, and system events. Missing historical settings resolve to all enabled.")
	public ApiResponse<NotificationPreferenceResponse> getMyPreferences() {
		return notificationService.getMyPreferences();
	}

	@PutMapping("/preferences")
	@Operation(summary = "Update notification preferences", description = "Replace all three delivery settings. Send the complete request body; omitted values are rejected instead of retaining a prior value.")
	public ApiResponse<NotificationPreferenceResponse> updateMyPreferences(
			@Valid @RequestBody NotificationPreferenceRequest request) {
		return notificationService.updateMyPreferences(request);
	}

	@PatchMapping("/{id}/read")
	@Operation(summary = "Mark a notification as read", description = "Mark one notification owned by the current user as read. Repeating the request is safe.")
	public ApiResponse<Void> markAsRead(@Parameter(description = "Notification ID") @PathVariable Long id) {
		return notificationService.markAsRead(id);
	}

	@PatchMapping("/read-all")
	@Operation(summary = "Mark all notifications as read", description = "Mark every unread notification in the current user's inbox as read.")
	public ApiResponse<Void> markAllAsRead() {
		return notificationService.markAllAsRead();
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a notification", description = "Permanently remove one notification owned by the current user. A foreign or missing ID returns 404.")
	public ApiResponse<Void> deleteNotification(@Parameter(description = "Notification ID") @PathVariable Long id) {
		return notificationService.deleteNotification(id);
	}

	@DeleteMapping("/read")
	@Operation(summary = "Clear read notifications", description = "Permanently remove all read notifications from the current user's inbox. Unread items remain untouched.")
	public ApiResponse<Void> clearReadNotifications() {
		return notificationService.clearReadNotifications();
	}
}
