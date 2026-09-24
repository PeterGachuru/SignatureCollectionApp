package ke.co.signature.DebtClassification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DebtClassificationRepository
        extends JpaRepository<DebtClassification, Long> {

    List<DebtClassification> findByActiveTrueOrderByMinDaysLateAsc();

    @Query("""
        SELECT dc FROM DebtClassification dc
        WHERE dc.active = true
        AND :daysLate >= dc.minDaysLate
        AND (dc.maxDaysLate IS NULL OR :daysLate <= dc.maxDaysLate)
    """)
    Optional<DebtClassification> findClassification(Integer daysLate);
}