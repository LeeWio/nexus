package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.DailyAnalytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAnalyticsRepository extends JpaRepository<DailyAnalytics, Long> {
    Optional<DailyAnalytics> findByStatDate(LocalDate statDate);
    List<DailyAnalytics> findByStatDateGreaterThanEqualOrderByStatDateAsc(LocalDate start);
}
