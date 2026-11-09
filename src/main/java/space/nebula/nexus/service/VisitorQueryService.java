package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.enums.TrafficClass;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.VisitorSummaryResponse;
import space.nebula.nexus.payload.response.VisitorTimelineResponse;
import space.nebula.nexus.repository.PageViewRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin reads over stored page views. Reader traffic is the default. */
@Service
@RequiredArgsConstructor
public class VisitorQueryService {

	private final PageViewRepository pageViewRepository;

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<VisitorSummaryResponse>> listVisitors(int days, int page, int size,
			boolean includeOwner, boolean includeBots) {
		int lookback = Math.clamp(days, 1, 365);
		int pageSize = Math.clamp(size, 1, 100);
		int pageIndex = Math.max(page, 0);
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(lookback).toLocalDate().atStartOfDay();
		List<String> classes = classNames(includeOwner, includeBots);
		long total = pageViewRepository.countVisitorRows(start, end, classes);
		List<VisitorSummaryResponse> rows = pageViewRepository
				.findVisitors(start, end, classes, PageRequest.of(pageIndex, pageSize)).stream().map(this::toSummary)
				.toList();
		int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);
		return ApiResponse.success(new PageResult<>(rows, total, pageIndex + 1, pageSize, totalPages));
	}

	@Transactional(readOnly = true)
	public ApiResponse<List<VisitorTimelineResponse>> timeline(String visitorHash, int days, boolean includeOwner,
			boolean includeBots) {
		int lookback = Math.clamp(days, 1, 365);
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(lookback).toLocalDate().atStartOfDay();
		List<PageView> views = pageViewRepository
				.findByVisitorHashAndViewedAtGreaterThanEqualAndViewedAtLessThanAndTrafficClassInOrderByViewedAtDesc(
						visitorHash, start, end, classes(includeOwner, includeBots), PageRequest.of(0, 200));
		return ApiResponse.success(views.stream()
				.map(view -> new VisitorTimelineResponse(view.getViewedAt(), view.getPath(), view.getTitle(),
						view.getClientName(), view.getDeviceType(), view.getSourceChannel(), view.getReferrerHost(),
						view.getIpAddress()))
				.toList());
	}

	private VisitorSummaryResponse toSummary(Map<String, Object> row) {
		return new VisitorSummaryResponse(text(row, "visitorHash"), text(row, "ipAddress"), text(row, "country"),
				text(row, "region"), text(row, "city"), text(row, "clientName"), text(row, "osName"),
				text(row, "deviceType"), time(row.get("firstSeen")), time(row.get("lastSeen")), number(row, "views"),
				number(row, "sessions"), number(row, "pages"));
	}

	private List<TrafficClass> classes(boolean includeOwner, boolean includeBots) {
		List<TrafficClass> classes = new ArrayList<>();
		classes.add(TrafficClass.HUMAN);
		if (includeOwner) {
			classes.add(TrafficClass.OWNER);
		}
		if (includeBots) {
			classes.add(TrafficClass.BOT);
		}
		return classes;
	}

	private List<String> classNames(boolean includeOwner, boolean includeBots) {
		return classes(includeOwner, includeBots).stream().map(Enum::name).toList();
	}

	private String text(Map<String, Object> row, String key) {
		Object value = row.get(key);
		if (value == null) {
			value = row.get(key.toLowerCase());
		}
		return value == null ? null : String.valueOf(value);
	}

	private long number(Map<String, Object> row, String key) {
		Object value = row.get(key);
		if (value == null) {
			value = row.get(key.toLowerCase());
		}
		return value instanceof Number number ? number.longValue() : 0L;
	}

	private LocalDateTime time(Object value) {
		if (value instanceof LocalDateTime dateTime) {
			return dateTime;
		}
		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime();
		}
		return null;
	}
}
