package space.nebula.nexus.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatsResponse {
    private long totalPosts;
    private long totalComments;
    private long totalViews;
    private long runtimeDays;
}
