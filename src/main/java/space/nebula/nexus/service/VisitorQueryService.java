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

import java.util.ArrayList;
import java.util.List;

/** Admin reads over stored page views. Reader traffic is the default. */
@Service
@RequiredArgsConstructor
public class VisitorQueryService {

	private final PageViewRepository pageViewRepository;
	private final AnalyticsTime analyticsTime;

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<VisitorSummaryResponse>> listVisitors(int days, int page, int size,
			boolean includeOwner, boolean includeBots) {
		int lookback = Math.clamp(days, 1, 365);
		int pageSize = Math.clamp(size, 1, 100);
		int pageIndex = Math.max(page, 0);
		var window = analyticsTime.recentDays(lookback);
		List<String> classes = classNames(includeOwner, includeBots);
		long total = pageViewRepository.countVisitorRows(window.start(), window.end(), classes);
		List<VisitorSummaryResponse> rows = pageViewRepository.findVisitors(window.start(), window.end(),
				classes(includeOwner, includeBots), PageRequest.of(pageIndex, pageSize));
		int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);
		return ApiResponse.success(new PageResult<>(rows, total, pageIndex + 1, pageSize, totalPages));
	}

	@Transactional(readOnly = true)
	public ApiResponse<List<VisitorTimelineResponse>> timeline(String visitorHash, int days, boolean includeOwner,
			boolean includeBots) {
		int lookback = Math.clamp(days, 1, 365);
		var window = analyticsTime.recentDays(lookback);
		List<PageView> views = pageViewRepository
				.findByVisitorHashAndViewedAtGreaterThanEqualAndViewedAtLessThanAndTrafficClassInOrderByViewedAtDesc(
						visitorHash, window.start(), window.end(), classes(includeOwner, includeBots),
						PageRequest.of(0, 200));
		return ApiResponse.success(views.stream()
				.map(view -> new VisitorTimelineResponse(view.getViewedAt(), view.getPath(), view.getTitle(),
						view.getClientName(), view.getDeviceType(), view.getSourceChannel(), view.getReferrerHost(),
						view.getIpAddress()))
				.toList());
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

}
