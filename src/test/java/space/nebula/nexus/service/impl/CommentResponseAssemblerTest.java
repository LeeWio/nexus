package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.mapper.CommentMapper;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentResponseAssemblerTest
{

	@Mock
	private CommentMapper commentMapper;
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private UserRepository userRepository;

	@Test
	void toResponseListBatchLoadsReplyCountsAndLikedState()
	{
		Comment first = comment(10L, 5L);
		Comment second = comment(11L, 0L);
		User user = new User();
		user.setId(7L);

		when(commentMapper.toResponse(first)).thenReturn(mapped(10L, 5L));
		when(commentMapper.toResponse(second)).thenReturn(mapped(11L, 0L));
		when(commentRepository.countRepliesByParentIds(List.of(10L, 11L), CommentStatus.APPROVED))
				.thenReturn(List.of(replyCount(10L, 3L)));
		when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
		when(commentRepository.findLikedCommentIds(7L, List.of(10L, 11L))).thenReturn(List.of(11L));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class))
		{
			mockedSecurity.when(SecurityUtil::getCurrentUsername).thenReturn("reader");

			CommentResponseAssembler assembler = new CommentResponseAssembler(commentMapper, commentRepository,
					userRepository);
			List<CommentResponse> responses = assembler.toResponseList(List.of(first, second));

			assertEquals(2, responses.size());
			assertEquals(3, responses.getFirst().replyCount());
			assertFalse(responses.getFirst().likedByCurrentUser());
			assertEquals(0, responses.get(1).replyCount());
			assertTrue(responses.get(1).likedByCurrentUser());
		}
	}

	@Test
	void toResponseListSkipsLikedLookupWhenAnonymous()
	{
		Comment comment = comment(10L, 2L);

		when(commentMapper.toResponse(comment)).thenReturn(mapped(10L, 2L));
		when(commentRepository.countRepliesByParentIds(List.of(10L), CommentStatus.APPROVED))
				.thenReturn(List.of());

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class))
		{
			mockedSecurity.when(SecurityUtil::getCurrentUsername).thenReturn(null);

			CommentResponseAssembler assembler = new CommentResponseAssembler(commentMapper, commentRepository,
					userRepository);
			List<CommentResponse> responses = assembler.toResponseList(List.of(comment));

			assertEquals(0, responses.getFirst().replyCount());
			assertFalse(responses.getFirst().likedByCurrentUser());
			verify(userRepository, never()).findByUsername(eq("reader"));
			verify(commentRepository, never()).findLikedCommentIds(eq(7L), anyCollection());
		}
	}

	private Comment comment(Long id, Long likesCount)
	{
		Comment comment = new Comment();
		comment.setId(id);
		comment.setLikesCount(likesCount);
		return comment;
	}

	private CommentResponse mapped(Long id, Long likesCount)
	{
		return CommentResponse.builder()
				.id(id)
				.likesCount(likesCount)
				.reportsCount(0L)
				.build();
	}

	private CommentRepository.CommentReplyCountView replyCount(Long parentId, Long replyCount)
	{
		return new CommentRepository.CommentReplyCountView()
		{
			@Override
			public Long getParentId()
			{
				return parentId;
			}

			@Override
			public Long getReplyCount()
			{
				return replyCount;
			}
		};
	}
}
