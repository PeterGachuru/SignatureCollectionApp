package ke.co.signature.Dashboard;

import ke.co.signature.Configs.Region.Region;
import ke.co.signature.Configs.Region.RegionRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecord;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentRepository;
import ke.co.signature.Payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class Dashboardservice {
    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;
    private final DebtAgeingUploadRepository debtAgeingUploadRepository;
    private final DebtAgeingRecordRepository debtAgeingRecordRepository;
    private final RegionRepository regionRepository;

    public Dashboardservice(PaymentRepository paymentRepository,
                            PaymentInProgressRepository paymentInProgressRepository,
                            DebtAgeingUploadRepository debtAgeingUploadRepository,
                            DebtAgeingRecordRepository debtAgeingRecordRepository,
                            RegionRepository regionRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentInProgressRepository = paymentInProgressRepository;
        this.debtAgeingUploadRepository = debtAgeingUploadRepository;
        this.debtAgeingRecordRepository = debtAgeingRecordRepository;
        this.regionRepository = regionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        return getDashboardSummary(null);
    }

    /** Builds the dashboard for all regions when regionId is null, otherwise one region. */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(Long regionId) {
        DebtAgeingUpload latestUpload = debtAgeingUploadRepository.findTopByOrderByUploadedAtDesc().orElse(null);

        BigDecimal totalDebtPending = BigDecimal.ZERO;
        long customersWhoHavePaid = 0;
        long customersYetToStartPaying = 0;
        long totalCustomers = 0;
        long totalPendingPayments = 0;
        long totalPostedPayments = 0;
        BigDecimal totalAmountPaid = BigDecimal.ZERO;
        List<DebtClassificationSummaryDTO> classificationSummary = new ArrayList<>();

        Long latestUploadId = null;
        String latestUploadFileName = null;
        java.time.LocalDate latestReportDate = null;
        java.time.LocalDateTime latestUploadedAt = null;
        String latestUploadedBy = null;

        if (latestUpload != null) {
            latestUploadId = latestUpload.getId();
            latestUploadFileName = latestUpload.getFileName();
            latestReportDate = latestUpload.getReportDate();
            latestUploadedAt = latestUpload.getUploadedAt();
            latestUploadedBy = latestUpload.getUploadedBy();

            List<DebtAgeingRecord> records = debtAgeingRecordRepository
                    .findByUploadIdOrderByTotalDebtDesc(latestUpload.getId());

            if (regionId != null) {
                records = records.stream()
                        .filter(r -> r.getCustomer() != null
                                && r.getCustomer().getTown() != null
                                && r.getCustomer().getTown().getRegion() != null
                                && regionId.equals(r.getCustomer().getTown().getRegion().getId()))
                        .toList();
            }

            totalCustomers = records.size();
            List<Long> customerIds = records.stream()
                    .map(r -> r.getCustomer().getId())
                    .distinct()
                    .toList();

            totalDebtPending = records.stream()
                    .map(DebtAgeingRecord::getTotalDebt)
                    .map(this::safeBigDecimal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal current = records.stream().map(r -> safeBigDecimal(r.getCurrentAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal days30 = records.stream().map(r -> safeBigDecimal(r.getDays30())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal days60 = records.stream().map(r -> safeBigDecimal(r.getDays60())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal days90 = records.stream().map(r -> safeBigDecimal(r.getDays90())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal days120 = records.stream().map(r -> safeBigDecimal(r.getDays120())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal over120 = records.stream().map(r -> safeBigDecimal(r.getOver120())).reduce(BigDecimal.ZERO, BigDecimal::add);

            classificationSummary.add(new DebtClassificationSummaryDTO("Current", 0, 0, current, calculatePercentage(current, totalDebtPending)));
            classificationSummary.add(new DebtClassificationSummaryDTO("30 Days", 30, 0, days30, calculatePercentage(days30, totalDebtPending)));
            classificationSummary.add(new DebtClassificationSummaryDTO("60 Days", 60, 0, days60, calculatePercentage(days60, totalDebtPending)));
            classificationSummary.add(new DebtClassificationSummaryDTO("90 Days", 90, 0, days90, calculatePercentage(days90, totalDebtPending)));
            classificationSummary.add(new DebtClassificationSummaryDTO("120 Days", 120, 0, days120, calculatePercentage(days120, totalDebtPending)));
            classificationSummary.add(new DebtClassificationSummaryDTO("Over 120 Days", 121, 0, over120, calculatePercentage(over120, totalDebtPending)));

            if (!customerIds.isEmpty()) {
                customersWhoHavePaid = paymentRepository.findCustomersWhoHavePaid(customerIds).size();
                customersYetToStartPaying = Math.max(0, totalCustomers - customersWhoHavePaid);
                totalAmountPaid = safeBigDecimal(paymentRepository.sumAmountByCustomerIds(customerIds));
                totalPostedPayments = paymentRepository.countByCustomerIdIn(customerIds);
                totalPendingPayments = paymentInProgressRepository.countByCustomerIdInAndStatusIn(
                        customerIds,
                        List.of(PaymentStatus.INITIATED, PaymentStatus.AWAITING_CONFIRMATION, PaymentStatus.READY_TO_POST));
            }
        }

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

    @Transactional(readOnly = true)
    public List<Region> getRegions() {
        return regionRepository.findAllByOrderByNameAsc();
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return safeBigDecimal(amount).multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
