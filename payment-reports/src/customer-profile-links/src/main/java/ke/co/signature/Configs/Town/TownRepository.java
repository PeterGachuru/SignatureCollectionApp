package ke.co.signature.Configs.Town;


import ke.co.signature.Configs.Region.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TownRepository extends JpaRepository<Town, Long> {
    Optional<Town> findByNameIgnoreCaseAndRegion(
            String name,
            Region region
    );
}