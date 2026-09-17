package ke.co.signature.Configs.CustomerUnit;

import ke.co.signature.Configs.Region.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerUnitRepository extends JpaRepository<CustomerUnit, Long> {
    Optional<CustomerUnit> findByNameIgnoreCase(String name);
}