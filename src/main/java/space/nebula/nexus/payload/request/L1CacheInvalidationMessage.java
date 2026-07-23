package space.nebula.nexus.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message broadcast to all instances to clear their local L1 (Caffeine) cache.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class L1CacheInvalidationMessage implements Serializable {
	private String cacheName;
	private String key; // null means clear all in this cacheName
	private boolean clearAll;
}
