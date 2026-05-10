package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Config;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
    Optional<Config> findByConfigKey(String configKey);
    List<Config> findByIsPublicTrue();
    boolean existsByConfigKey(String configKey);
}
