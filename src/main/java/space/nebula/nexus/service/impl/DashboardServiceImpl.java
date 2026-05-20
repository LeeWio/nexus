package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.DashboardStatsResponse;
import space.nebula.nexus.payload.response.PublicStatsResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.ConfigRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IDashboardService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final ConfigRepository configRepository;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.SITE_STATS, key = "'admin_dashboard'")
	public ApiResponse<DashboardStatsResponse> getStatistics() {
		long totalUsers = userRepository.count();
		long totalPosts = postRepository.count();
		long totalComments = commentRepository.count();
		long pendingComments = commentRepository.countByStatus(CommentStatus.PENDING);

		Long viewsSum = postRepository.sumTotalViews();
		long totalViews = (viewsSum != null) ? viewsSum : 0L;

		DashboardStatsResponse stats = DashboardStatsResponse.builder().totalUsers(totalUsers).totalPosts(totalPosts)
				.totalComments(totalComments).pendingComments(pendingComments).totalViews(totalViews).build();

		return ApiResponse.success("Statistics retrieved successfully", stats);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.SITE_STATS, key = CacheConstants.PUBLIC_DASHBOARD_KEY)
	public ApiResponse<PublicStatsResponse> getPublicStatistics() {
		long totalPosts = postRepository.count();
		long totalComments = commentRepository.countByStatus(CommentStatus.APPROVED);

		Long viewsSum = postRepository.sumTotalViews();
		long totalViews = (viewsSum != null) ? viewsSum : 0L;

		long runtimeDays = calculateRuntimeDays();

		PublicStatsResponse stats = PublicStatsResponse.builder().totalPosts(totalPosts).totalComments(totalComments)
				.totalViews(totalViews).runtimeDays(runtimeDays).build();

		return ApiResponse.success("Public statistics retrieved successfully", stats);
	}

	private long calculateRuntimeDays() {
		return configRepository.findByConfigKey("site_launch_date").map(config -> {
			try {
				LocalDate launchDate = LocalDate.parse(config.getConfigValue());
				return ChronoUnit.DAYS.between(launchDate, LocalDate.now());
			} catch (Exception e) {
				log.warn("Failed to parse site_launch_date: {}", config.getConfigValue());
				return 0L;
			}
		}).orElse(0L);
	}
}
