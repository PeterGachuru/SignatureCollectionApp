package ke.co.signature.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardSummaryDTO {

    /*
     * LATEST AGEING DATA
     */

    /**
     * Total outstanding debt from the latest ageing upload.
     */
    private BigDecimal totalDebtPending;

    /**
     * Number of customers in the latest ageing upload
     * who have made at least one posted payment.
     */
    private long customersWhoHavePaid;

    /**
     * Number of customers in the latest ageing upload
     * who have never made a posted payment.
     */
    private long customersYetToStartPaying;

    /**
     * Total amount of all posted payments.
     */
    private BigDecimal totalAmountPaid;

    /**
     * Number of customers listed in the latest ageing upload.
     */
    private long totalCustomers;

    /**
     * Number of payments waiting to be posted.
     */
    private long totalPendingPayments;

    /**
     * Number of successfully posted payments.
     */
    private long totalPostedPayments;


    /*
     * DEBT CLASSIFICATION
     */

    private List<DebtClassificationSummaryDTO>
            classificationSummary;


    /*
     * LATEST UPLOAD INFORMATION
     */

    private Long latestUploadId;

    private String latestUploadFileName;

    private LocalDate latestReportDate;

    private LocalDateTime latestUploadedAt;

    private String latestUploadedBy;
}