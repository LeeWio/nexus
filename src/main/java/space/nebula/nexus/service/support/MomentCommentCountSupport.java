package space.nebula.nexus.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.repository.CommentRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Attaches approved comment totals onto moment API responses.
 */
@Component
@RequiredArgsConstructor
public class MomentCommentCountSupport {

	private final CommentRepository commentRepository;

	public MomentResponse withCount(MomentResponse response) {
		if (response == null || response.id() == null) {
			return response;
		}
		long count = commentRepository.countByMomentIdAndStatus(response.id(), CommentStatus.APPROVED);
		return copyWithCommentsCount(response, count);
	}

	public List<MomentResponse> withCounts(Collection<MomentResponse> responses) {
		if (responses == null || responses.isEmpty()) {
			return List.of();
		}
		List<Long> momentIds = responses.stream().map(MomentResponse::id).filter(Objects::nonNull).distinct()
				.toList();
		if (momentIds.isEmpty()) {
			return List.copyOf(responses);
		}

		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : commentRepository.countApprovedCommentsByMomentIds(momentIds, CommentStatus.APPROVED)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return responses.stream()
				.map(response -> copyWithCommentsCount(response, counts.getOrDefault(response.id(), 0L))).toList();
	}

	private static MomentResponse copyWithCommentsCount(MomentResponse response, long commentsCount) {
		return new MomentResponse(response.id(), response.content(), response.stockSymbol(), response.likesCount(),
				commentsCount, response.visibility(), response.authorName(), response.authorAvatar(), response.images(),
				response.topics(), response.createdAt(), response.updatedAt());
	}
}
