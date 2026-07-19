package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request payload for approving a post for future publication.
 *
 * @param scheduledAt future publication time in the server's configured time zone
 */
@Schema(description = "Future publication schedule")
public record PostScheduleRequest(
		@NotNull(message = "A publication time is required")
		@Future(message = "The publication time must be in the future")
		@Schema(description = "Planned publication time", example = "2026-07-20T09:00:00")
		LocalDateTime scheduledAt)
{
}
