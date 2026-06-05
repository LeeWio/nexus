package space.nebula.nexus.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.security.model.SecurityUser;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom Permission Evaluator for Fine-grained RBAC and Object-Level
 * Authorization. Allows using annotations
 * like: @PreAuthorize("hasPermission(#postId, 'Post', 'APPROVE')")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NexusPermissionEvaluator implements PermissionEvaluator
{

	private final PostRepository postRepository;

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission)
	{
		if (authentication == null || targetDomainObject == null || !(permission instanceof String permString))
		{
			return false;
		}

		// Example: Check permission if the actual domain object is passed
		if (targetDomainObject instanceof Post post)
		{
			return hasPrivilegeForPost(authentication, post, permString);
		}

		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission)
	{
		if (authentication == null || targetId == null || targetType == null
				|| !(permission instanceof String permString))
		{
			return false;
		}

		if ("Post".equalsIgnoreCase(targetType))
		{
			Long postId = Long.valueOf(targetId.toString());
			Post post = postRepository.findById(postId)
					.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
			return hasPrivilegeForPost(authentication, post, permString);
		}

		return false;
	}

	private boolean hasPrivilegeForPost(Authentication authentication, Post post, String permission)
	{
		Set<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toSet());

		// ADMIN always has full access
		if (roles.contains("ROLE_ADMIN"))
		{
			return true;
		}

		if (!(authentication.getPrincipal() instanceof SecurityUser securityUser))
		{
			return false;
		}

		Long currentUserId = securityUser.getUser().getId();
		boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUserId);
		boolean isEditor = roles.contains("ROLE_EDITOR");

		return switch (permission.toUpperCase()) {
		case "READ" -> true; // Depends on status, but technically controlled by service logic
		case "EDIT", "DELETE" -> isAuthor || isEditor;
		case "SUBMIT" -> isAuthor;
		case "APPROVE", "REJECT" -> isEditor; // Only Editors (and Admins) can approve/reject
		default -> false;
		};
	}
}
