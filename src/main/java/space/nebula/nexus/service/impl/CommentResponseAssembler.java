package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.mapper.CommentMapper;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentResponseAssembler
{

	private final CommentMapper commentMapper;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	public CommentResponse toResponse(Comment comment)
	{
		return toResponseList(List.of(comment)).getFirst();
	}

	public List<CommentResponse> toResponseList(Collection<Comment> comments)
	{
		if (comments.isEmpty())
		{
			return List.of();
		}

		List<Long> commentIds = comments.stream().map(Comment::getId).toList();
		Map<Long, Long> replyCounts = loadReplyCounts(commentIds);
		Set<Long> likedCommentIds = loadLikedCommentIds(commentIds);

		return comments.stream()
				.map(comment -> enrich(comment, replyCounts, likedCommentIds))
				.toList();
	}

	private Map<Long, Long> loadReplyCounts(List<Long> commentIds)
	{
		return commentRepository.countRepliesByParentIds(commentIds, CommentStatus.APPROVED)
				.stream()
				.collect(Collectors.toMap(CommentRepository.CommentReplyCountView::getParentId,
						CommentRepository.CommentReplyCountView::getReplyCount));
	}

	private Set<Long> loadLikedCommentIds(List<Long> commentIds)
	{
		String username = SecurityUtil.getCurrentUsername();
		if (username == null)
		{
			return Set.of();
		}

		return userRepository.findByUsername(username)
				.map(user -> Set.copyOf(commentRepository.findLikedCommentIds(user.getId(), commentIds)))
				.orElseGet(Set::of);
	}

	private CommentResponse enrich(Comment comment, Map<Long, Long> replyCounts, Set<Long> likedCommentIds)
	{
		CommentResponse response = commentMapper.toResponse(comment);
		return CommentResponse.builder()
				.id(response.id())
				.parentId(response.parentId())
				.content(response.content())
				.username(response.username())
				.nickname(response.nickname())
				.avatar(response.avatar())
				.status(response.status())
				.postId(response.postId())
				.postTitle(response.postTitle())
				.likesCount(response.likesCount() == null ? 0L : response.likesCount())
				.reportsCount(response.reportsCount() == null ? 0L : response.reportsCount())
				.replyCount(Math.toIntExact(replyCounts.getOrDefault(comment.getId(), 0L)))
				.likedByCurrentUser(likedCommentIds.contains(comment.getId()))
				.pinned(Boolean.TRUE.equals(response.pinned()))
				.featured(Boolean.TRUE.equals(response.featured()))
				.deletedPlaceholder(Boolean.TRUE.equals(response.deletedPlaceholder()))
				.createdAt(response.createdAt())
				.editedAt(response.editedAt())
				.build();
	}
}
