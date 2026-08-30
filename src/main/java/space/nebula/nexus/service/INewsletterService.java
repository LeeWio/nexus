package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.payload.response.NewsletterAudienceOverviewResponse;
import space.nebula.nexus.payload.response.NewsletterSubscriberResponse;
import space.nebula.nexus.payload.response.PageResult;

import org.springframework.data.domain.Pageable;

public interface INewsletterService {
	ApiResponse<Void> subscribe(String email);
	ApiResponse<Void> verify(String token);
	ApiResponse<Void> unsubscribe(String token);
	ApiResponse<NewsletterAudienceOverviewResponse> getAudienceOverview();
	ApiResponse<PageResult<NewsletterSubscriberResponse>> getSubscribers(SubscriberStatus status, String query,
			Pageable pageable);
	void sendWeeklyNewsletter();
}
