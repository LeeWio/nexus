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
			@Parameter(description = "Inbox view: inbox, saved, or done", example = "inbox") @RequestParam(defaultValue = "inbox") String view,
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return notificationService.getMyNotifications(unreadOnly, view, pageable);
	}

	@GetMapping("/unread/count")
	@Operation(summary = "Get unread count", description = "Retrieve the current user's unread inbox count for notification badges.")
	public ApiResponse<Long> getUnreadCount() {
		return notificationService.getUnreadCount();
	}

	@GetMapping("/preferences")
	@Operation(summary = "Get notification preferences", description = "Retrieve in-app and email delivery settings for comment activity, followed-category posts, and system events. Missing historical settings keep in-app delivery enabled and email delivery disabled.")
	public ApiResponse<NotificationPreferenceResponse> getMyPreferences() {
		return notificationService.getMyPreferences();
	}

	@PutMapping("/preferences")
	@Operation(summary = "Update notification preferences", description = "Replace the in-app delivery settings and, when supplied, the corresponding email settings. The three in-app values are required; omitted email values retain their existing state for legacy clients.")
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

	@PatchMapping("/{id}/done")
	@Operation(summary = "Complete a notification", description = "Remove one notification from the active inbox while keeping it available in Done history.")
	public ApiResponse<Void> markAsDone(@Parameter(description = "Notification ID") @PathVariable Long id) {
		return notificationService.markAsDone(id);
	}

	@PatchMapping("/{id}/reopen")
	@Operation(summary = "Reopen a notification", description = "Move one completed notification back into the active inbox without changing its read state.")
	public ApiResponse<Void> reopen(@Parameter(description = "Notification ID") @PathVariable Long id) {
		return notificationService.reopen(id);
	}

	@PatchMapping("/{id}/saved")
	@Operation(summary = "Save or unsave a notification", description = "Keep a notification in the Saved view for deliberate follow-up.")
	public ApiResponse<Void> setSaved(@Parameter(description = "Notification ID") @PathVariable Long id,
			@RequestParam boolean saved) {
		return notificationService.setSaved(id, saved);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a notification", description = "Permanently remove one notification owned by the current user. A foreign or missing ID returns 404.")
	public ApiResponse<Void> deleteNotification(@Parameter(description = "Notification ID") @PathVariable Long id) {
		return notificationService.deleteNotification(id);
	}

	@DeleteMapping("/read")
	@Operation(summary = "Clear read notifications", description = "Permanently remove read, unsaved notifications from the active inbox. Saved and completed history remain untouched.")
	public ApiResponse<Void> clearReadNotifications() {
		return notificationService.clearReadNotifications();
	}
}
