package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;

public interface INewsletterService {
	ApiResponse<Void> subscribe(String email);
	ApiResponse<Void> verify(String token);
	ApiResponse<Void> unsubscribe(String token);
	void sendWeeklyNewsletter();
}
