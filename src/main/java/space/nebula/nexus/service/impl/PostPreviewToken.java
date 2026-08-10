package space.nebula.nexus.service.impl;

import java.util.Objects;

import space.nebula.nexus.enums.PostStatus;

/**
 * State bound to a short-lived post preview token.
 */
record PostPreviewToken(Long postId, String contentHash, PostStatus status) {

	boolean matches(Long currentPostId, String currentContentHash, PostStatus currentStatus) {
		return postId != null && postId.equals(currentPostId) && Objects.equals(contentHash, currentContentHash)
				&& status == currentStatus;
	}
}
