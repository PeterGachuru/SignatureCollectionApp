package ke.co.signature.CustomerDashboard;

import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.Payment.PostedPayment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CustomerDashboardDTO {

    private BigDecimal totalPending;
    private BigDecimal totalOverpayments;
    private long outstandingSalesCount;
    private List<PostedPayment> recentPostedPayments;
    private List<CreditSale> recentPurchases;
}

