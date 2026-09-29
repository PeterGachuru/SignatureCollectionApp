package ke.co.signature.CustomerDashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerPaymentSummaryDTO {

    private BigDecimal totalPosted;
    private BigDecimal totalUnallocated;
    private BigDecimal totalAllocated;
    private long postedPaymentCount;

    private BigDecimal pendingAmount;
    private long pendingPaymentCount;
}
