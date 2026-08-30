package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.response.EditorialCalendarResponse;
import space.nebula.nexus.payload.response.EditorialCalendarResponse.Entry;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.service.IEditorialCalendarService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EditorialCalendarServiceImpl implements IEditorialCalendarService {

	private final PostRepository postRepository;
	private final MomentRepository momentRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<EditorialCalendarResponse> getCalendar(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			LocalDate today = LocalDate.now();
			from = today.withDayOfMonth(1);
			to = from.plusMonths(1).minusDays(1);
		}
		if (to.isBefore(from) || to.isAfter(from.plusMonths(3).minusDays(1))) {
			throw new IllegalArgumentException(
					"Editorial calendar range must be valid and no longer than three months");
		}

		LocalDateTime start = from.atStartOfDay();
		LocalDateTime end = to.plusDays(1).atStartOfDay().minusNanos(1);
		List<Entry> entries = new ArrayList<>();
		postRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(start, end).stream()
				.map(post -> toPostEntry(post, post.getScheduledAt())).forEach(entries::add);
		postRepository.findByPublishedAtBetweenOrderByPublishedAtAsc(start, end).stream()
				.map(post -> toPostEntry(post, post.getPublishedAt())).forEach(entries::add);
		momentRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end).stream().map(this::toMomentEntry)
				.forEach(entries::add);
		entries.sort(Comparator.comparing(Entry::timestamp).thenComparing(Entry::id));

		return ApiResponse.success("Editorial calendar retrieved successfully",
				new EditorialCalendarResponse(from, to, entries));
	}

	private Entry toPostEntry(Post post, LocalDateTime timestamp) {
		return new Entry("post-" + post.getId(), "POST", post.getTitle(), timestamp.toLocalDate(), timestamp,
				post.getStatus(), "/posts?id=" + post.getId());
	}

	private Entry toMomentEntry(Moment moment) {
		LocalDateTime timestamp = moment.getCreatedAt();
		return new Entry("moment-" + moment.getId(), "MOMENT", excerpt(moment.getContent()), timestamp.toLocalDate(),
				timestamp, null, "/moments?id=" + moment.getId());
	}

	private String excerpt(String content) {
		if (content == null || content.isBlank()) {
			return "Untitled moment";
		}
		String normalized = content.replaceAll("\\s+", " ").trim();
		return normalized.length() > 100 ? normalized.substring(0, 97) + "..." : normalized;
	}
}
