package ke.co.signature.DebtClassification;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtClassificationService {

    private final DebtClassificationRepository repository;

    public DebtClassificationService(DebtClassificationRepository repository) {
        this.repository = repository;
    }

    public List<DebtClassification> findAll() {
        return repository.findAll();
    }

    public List<DebtClassification> findActive() {
        return repository.findByActiveTrueOrderByMinDaysLateAsc();
    }

    public DebtClassification getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Debt classification not found"));
    }

    public DebtClassification save(DebtClassification classification) {

        if (classification.getMinDaysLate() < 0) {
            throw new IllegalArgumentException("Min days cannot be negative");
        }

        if (classification.getMaxDaysLate() != null &&
                classification.getMaxDaysLate() < classification.getMinDaysLate()) {
            throw new IllegalArgumentException("Max days must be greater than min days");
        }

        return repository.save(classification);
    }
}