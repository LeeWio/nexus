package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Summary of a user-owned post collection.
 *
 * @param id collection identifier
 * @param name collection name
 * @param description optional description
 * @param itemCount number of posts in the collection
 * @param createdAt creation time
 * @param updatedAt last modification time
 */
@Schema(description = "Personal post collection summary")
public record PostCollectionResponse(Long id, String name, String description, Long itemCount,
		LocalDateTime createdAt, LocalDateTime updatedAt)
{
}
