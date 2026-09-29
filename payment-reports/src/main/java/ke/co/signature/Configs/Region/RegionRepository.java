package ke.co.signature.Configs.Region;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByNameIgnoreCase(String name);

    java.util.List<Region> findAllByOrderByNameAsc();
}