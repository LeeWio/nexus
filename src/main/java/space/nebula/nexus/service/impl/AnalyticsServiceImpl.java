package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.service.IAnalyticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements IAnalyticsService {

    private final VisitLogRepository visitLogRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.ANALYTICS, key = CacheConstants.OVERVIEW_KEY)
    public ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);

        long todayPv = visitLogRepository.countPv(todayStart, todayEnd);
        long todayUv = visitLogRepository.countUv(todayStart, todayEnd);
        
        long yesterdayPv = visitLogRepository.countPv(yesterdayStart, yesterdayEnd);
        long yesterdayUv = visitLogRepository.countUv(yesterdayStart, yesterdayEnd);

        double growthRate = 0.0;
        if (yesterdayPv > 0) {
            growthRate = ((double)(todayPv - yesterdayPv) / yesterdayPv) * 100;
        }

        List<Map<String, Object>> topContent = visitLogRepository.findTopContent(todayStart);

        AnalyticsOverviewResponse response = AnalyticsOverviewResponse.builder()
                .todayPv(todayPv)
                .todayUv(todayUv)
                .yesterdayPv(yesterdayPv)
                .yesterdayUv(yesterdayUv)
                .pvGrowthRate(growthRate)
                .topContent(topContent)
                .build();

        return ApiResponse.success(response);
    }
}
