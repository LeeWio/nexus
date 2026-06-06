package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "blog_daily_analytics")
public class DailyAnalytics extends BaseEntity {

    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    @Column(nullable = false)
    private Long pv = 0L;

    @Column(nullable = false)
    private Long uv = 0L;

    @Column(name = "post_views", nullable = false)
    private Long postViews = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;
}
