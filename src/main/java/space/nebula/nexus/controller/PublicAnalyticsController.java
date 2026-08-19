package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.ContentAnalyticsEventRequest;
import space.nebula.nexus.service.AnalyticsPrivacyService;
import space.nebula.nexus.service.IAnalyticsService;

import java.util.concurrent.TimeUnit;

@Tag(name = "Public Analytics", description = "Anonymous first-party content engagement collection")
@RestController
@RequestMapping("/api/v1/public/analytics")
@RequiredArgsConstructor
public class PublicAnalyticsController {

	private final IAnalyticsService analyticsService;
	private final AnalyticsPrivacyService analyticsPrivacyService;

	@PostMapping("/content-events")
	@RateLimit(count = 60, time = 1, unit = TimeUnit.MINUTES, message = "Too many analytics events. Please retry shortly.")
	@Operation(summary = "Record an anonymous content event", description = "Records de-duplicated post impressions, clicks, and reading milestones. The API sets a first-party anonymous session cookie and never stores a raw IP address.")
	public ApiResponse<Void> recordContentEvent(@Valid @RequestBody ContentAnalyticsEventRequest request,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		String sessionId = analyticsPrivacyService.resolveSessionId(servletRequest, servletResponse);
		String visitorHash = analyticsPrivacyService.hashVisitor(servletRequest);
		return analyticsService.recordContentEvent(request, sessionId, visitorHash);
	}
}
