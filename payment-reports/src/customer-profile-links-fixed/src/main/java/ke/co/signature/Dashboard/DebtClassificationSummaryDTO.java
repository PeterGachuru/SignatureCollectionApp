package ke.co.signature.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DebtClassificationSummaryDTO {

    private String classificationName;

    private Integer minDaysLate;

    private long count;

    private BigDecimal totalAmount;

    private BigDecimal percentage;
}