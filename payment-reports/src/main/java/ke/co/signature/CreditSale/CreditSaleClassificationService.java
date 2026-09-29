package ke.co.signature.CreditSale;

import ke.co.signature.DebtClassification.DebtClassification;
import ke.co.signature.DebtClassification.DebtClassificationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class CreditSaleClassificationService {

    private final DebtClassificationRepository classificationRepository;

    public CreditSaleClassificationService(DebtClassificationRepository classificationRepository) {
        this.classificationRepository = classificationRepository;
    }

    public DebtClassification classify(CreditSale cs) {

        if (cs.getBalance() == null || cs.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return null; // Fully paid → no classification
        }

        int daysLate = (int) ChronoUnit.DAYS.between(cs.getSaleDate(), LocalDate.now());

        return classificationRepository.findClassification(daysLate)
                .orElse(null);
    }
}