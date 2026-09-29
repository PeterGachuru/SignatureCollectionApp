package ke.co.signature.Dashboard;

import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class Dashboardservice {

    private final CustomerRepository customerRepository;

    private final PaymentRepository paymentRepository;

    private final PaymentInProgressRepository
            paymentInProgressRepository;

    private final DebtAgeingUploadRepository
            debtAgeingUploadRepository;

    private final DebtAgeingRecordRepository
            debtAgeingRecordRepository;


    public Dashboardservice(
            CustomerRepository customerRepository,
            PaymentRepository paymentRepository,
            PaymentInProgressRepository paymentInProgressRepository,
            DebtAgeingUploadRepository debtAgeingUploadRepository,
            DebtAgeingRecordRepository debtAgeingRecordRepository) {

        this.customerRepository =
                customerRepository;

        this.paymentRepository =
                paymentRepository;

        this.paymentInProgressRepository =
                paymentInProgressRepository;

        this.debtAgeingUploadRepository =
                debtAgeingUploadRepository;

        this.debtAgeingRecordRepository =
                debtAgeingRecordRepository;
    }


    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {

        /*
         * =========================================================
         * LATEST AGEING UPLOAD
         * =========================================================
         */

        DebtAgeingUpload latestUpload =
                debtAgeingUploadRepository
                        .findTopByOrderByUploadedAtDesc()
                        .orElse(null);


        /*
         * =========================================================
         * DEFAULT VALUES
         * =========================================================
         */

        BigDecimal totalDebtPending =
                BigDecimal.ZERO;

        long customersWhoHavePaid = 0;

        long customersYetToStartPaying = 0;

        long totalCustomers = 0;

        List<DebtClassificationSummaryDTO>
                classificationSummary =
                new ArrayList<>();


        Long latestUploadId = null;

        String latestUploadFileName = null;

        var latestReportDate = nullDate();

        var latestUploadedAt = nullDateTime();

        String latestUploadedBy = null;


        /*
         * =========================================================
         * PROCESS LATEST AGEING UPLOAD
         * =========================================================
         */

        if (latestUpload != null) {

            latestUploadId =
                    latestUpload.getId();

            latestUploadFileName =
                    latestUpload.getFileName();

            latestReportDate =
                    latestUpload.getReportDate();

            latestUploadedAt =
                    latestUpload.getUploadedAt();

            latestUploadedBy =
                    latestUpload.getUploadedBy();


            /*
             * =====================================================
             * TOTAL CUSTOMERS
             * =====================================================
             */

            totalCustomers =
                    debtAgeingRecordRepository
                            .countByUploadId(
                                    latestUpload.getId()
                            );


            /*
             * =====================================================
             * TOTAL OUTSTANDING DEBT
             * =====================================================
             */

            totalDebtPending =
                    debtAgeingRecordRepository
                            .getTotalDebtByUploadId(
                                    latestUpload.getId()
                            );

            if (totalDebtPending == null) {

                totalDebtPending =
                        BigDecimal.ZERO;
            }


            /*
             * =====================================================
             * DEBT CLASSIFICATION TOTALS
             * =====================================================
             */

            DebtClassificationTotalsDTO totals =
                    debtAgeingRecordRepository
                            .getClassificationTotals(
                                    latestUpload.getId()
                            );


            BigDecimal current =
                    safeBigDecimal(totals.getCurrent());

            BigDecimal days30 =
                    safeBigDecimal(totals.getDays30());

            BigDecimal days60 =
                    safeBigDecimal(totals.getDays60());

            BigDecimal days90 =
                    safeBigDecimal(totals.getDays90());

            BigDecimal days120 =
                    safeBigDecimal(totals.getDays120());

            BigDecimal over120 =
                    safeBigDecimal(totals.getOver120());


            /*
             * =====================================================
             * CALCULATE PERCENTAGES IN JAVA
             * =====================================================
             */

            BigDecimal currentPercentage =
                    calculatePercentage(
                            current,
                            totalDebtPending
                    );


            BigDecimal days30Percentage =
                    calculatePercentage(
                            days30,
                            totalDebtPending
                    );


            BigDecimal days60Percentage =
                    calculatePercentage(
                            days60,
                            totalDebtPending
                    );


            BigDecimal days90Percentage =
                    calculatePercentage(
                            days90,
                            totalDebtPending
                    );


            BigDecimal days120Percentage =
                    calculatePercentage(
                            days120,
                            totalDebtPending
                    );


            BigDecimal over120Percentage =
                    calculatePercentage(
                            over120,
                            totalDebtPending
                    );


            /*
             * =====================================================
             * BUILD CLASSIFICATION SUMMARY
             * =====================================================
             */

            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "Current",
                            0,
                            0,
                            current,
                            currentPercentage
                    )
            );


            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "30 Days",
                            30,
                            0,
                            days30,
                            days30Percentage
                    )
            );


            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "60 Days",
                            60,
                            0,
                            days60,
                            days60Percentage
                    )
            );


            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "90 Days",
                            90,
                            0,
                            days90,
                            days90Percentage
                    )
            );


            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "120 Days",
                            120,
                            0,
                            days120,
                            days120Percentage
                    )
            );


            classificationSummary.add(
                    new DebtClassificationSummaryDTO(
                            "Over 120 Days",
                            121,
                            0,
                            over120,
                            over120Percentage
                    )
            );


            /*
             * =====================================================
             * CUSTOMERS WHO HAVE PAID
             * =====================================================
             *
             * We only consider customers contained in the
             * latest ageing upload.
             */

            List<Long> customerIds =
                    debtAgeingRecordRepository
                            .findByUploadIdOrderByTotalDebtDesc(
                                    latestUpload.getId()
                            )
                            .stream()
                            .map(record ->
                                    record.getCustomer().getId()
                            )
                            .toList();


            if (!customerIds.isEmpty()) {

                customersWhoHavePaid =
                        paymentRepository
                                .countDistinctCustomersWhoHavePaid(
                                        customerIds
                                );


                customersYetToStartPaying =
                        totalCustomers -
                                customersWhoHavePaid;


                if (customersYetToStartPaying < 0) {

                    customersYetToStartPaying = 0;
                }
            }
        }


        /*
         * =========================================================
         * PAYMENT SUMMARY
         * =========================================================
         */

        long totalPendingPayments =
                paymentInProgressRepository.count();


        long totalPostedPayments =
                paymentRepository.count();


        BigDecimal totalAmountPaid =
                paymentRepository
                        .getTotalPostedAmount();


        if (totalAmountPaid == null) {

            totalAmountPaid =
                    BigDecimal.ZERO;
        }


        /*
         * =========================================================
         * RETURN DASHBOARD
         * =========================================================
         */

        return new DashboardSummaryDTO(

                totalDebtPending,

                customersWhoHavePaid,

                customersYetToStartPaying,

                totalAmountPaid,

                totalCustomers,

                totalPendingPayments,

                totalPostedPayments,

                classificationSummary,

                latestUploadId,

                latestUploadFileName,

                latestReportDate,

                latestUploadedAt,

                latestUploadedBy
        );
    }


    /*
     * =============================================================
     * CALCULATE PERCENTAGE
     * =============================================================
     *
     * Formula:
     *
     * classification amount
     * --------------------- × 100
     * total pending debt
     *
     * Result is rounded to 2 decimal places.
     */

    private BigDecimal calculatePercentage(
            BigDecimal amount,
            BigDecimal total) {

        if (amount == null) {

            amount = BigDecimal.ZERO;
        }


        if (total == null ||
                total.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }


        return amount
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        total,
                        2,
                        RoundingMode.HALF_UP
                );
    }


    /*
     * =============================================================
     * SAFE BIG DECIMAL
     * =============================================================
     */

    private BigDecimal safeBigDecimal(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    /*
     * =============================================================
     * NULL DATE HELPERS
     * =============================================================
     */

    private java.time.LocalDate nullDate() {

        return null;
    }


    private java.time.LocalDateTime nullDateTime() {

        return null;
    }
}