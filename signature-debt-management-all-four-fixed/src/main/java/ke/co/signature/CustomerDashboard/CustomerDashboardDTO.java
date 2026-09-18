package ke.co.signature.CustomerDashboard;

import ke.co.signature.Payment.PostedPayment;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CustomerDashboardDTO {

    private String businessName;
    private String customerCode;
    private String username;
    private String contactPerson;
    private String phone;
    private String email;
    private String location;
    private String unitName;
    private String townName;
    private String regionName;
    private Boolean active;

    /** Latest debt ageing information applicable to this customer. */
    private CustomerAgingDTO latestAging;

    /** Payment totals and pending payment information for this customer. */
    private CustomerPaymentSummaryDTO paymentSummary;

    /** Most recent successfully posted payments. */
    private List<PostedPayment> recentPostedPayments;

    /** Payments currently being processed / awaiting posting. */
    private List<PaymentInProgress> recentPendingPayments;

    /** Kept for compatibility with any existing customer dashboard references. */
    private BigDecimal totalPending;
    private BigDecimal totalOverpayments;
    private long outstandingSalesCount;
}
