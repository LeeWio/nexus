package space.nebula.nexus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.service.IAnalyticsService;
import space.nebula.nexus.payload.response.TrafficStatsResponse;
import space.nebula.nexus.common.ApiResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires a deterministic analytics fixture; excluded from the automated suite")
public class AnalyticsRangeTest {

    @Autowired
    private IAnalyticsService analyticsService;

    @Test
    public void testDifferentRanges() {
        System.out.println("--- STARTING DYNAMIC RANGE TEST ---");
        
        ApiResponse<TrafficStatsResponse> res7 = analyticsService.getTrafficStats(7);
        long total7 = (long) res7.data().summary().sessions().numericValue();
        int size7 = res7.data().timeSeries().size();
        System.out.println("7 Days: Total Sessions = " + total7 + ", TimeSeries Size = " + size7);

        ApiResponse<TrafficStatsResponse> res30 = analyticsService.getTrafficStats(30);
        long total30 = (long) res30.data().summary().sessions().numericValue();
        int size30 = res30.data().timeSeries().size();
        System.out.println("30 Days: Total Sessions = " + total30 + ", TimeSeries Size = " + size30);

        ApiResponse<TrafficStatsResponse> res365 = analyticsService.getTrafficStats(365);
        long total365 = (long) res365.data().summary().sessions().numericValue();
        int size365 = res365.data().timeSeries().size();
        System.out.println("365 Days: Total Sessions = " + total365 + ", TimeSeries Size = " + size365);

        assertTrue(total365 > total30, "365 days should have more data than 30 days");
        assertTrue(total30 > total7, "30 days should have more data than 7 days");
        assertEquals(7, size7, "TimeSeries for 7 days should have 7 entries");
        assertEquals(30, size30, "TimeSeries for 30 days should have 30 entries");
        
        System.out.println("--- DYNAMIC RANGE TEST PASSED ---");
    }
}
