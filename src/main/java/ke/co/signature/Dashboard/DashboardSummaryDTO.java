package ke.co.signature.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardSummaryDTO {
    private long totalPendingPayments;
    private long totalCustomers;
    private long activeCustomers90Days;

    private BigDecimal totalDueLoans;
    private Double totalDueLoans90Days;

    private BigDecimal totalPayments90Days;
    private BigDecimal totalOverpayments;
    private List<DebtClassificationSummaryDTO> classificationSummary;
}

