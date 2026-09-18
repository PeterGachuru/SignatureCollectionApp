package ke.co.signature.CustomerDashboard;

import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecord;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.Payment.PaymentRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CustomerRepository customerRepository;
    private final DebtAgeingUploadRepository debtAgeingUploadRepository;
    private final DebtAgeingRecordRepository debtAgeingRecordRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;

    @Transactional(readOnly = true)
    public CustomerDashboardDTO getDashboard(String username) {

        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No customer found for username: " + username));

        return buildDashboard(customer);
    }

    /**
     * Full customer dashboard used by administrators when viewing
     * a customer from the customer list.
     */
    @Transactional(readOnly = true)
    public CustomerDashboardDTO getDashboardForCustomer(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));

        return buildDashboard(customer);
    }

    private CustomerDashboardDTO buildDashboard(Customer customer) {

        Optional<DebtAgeingUpload> latestUpload =
                debtAgeingUploadRepository.findLatestUploadForCustomer(customer);

        CustomerAgingDTO latestAging = latestUpload
                .flatMap(upload -> debtAgeingRecordRepository
                        .findByUploadIdAndCustomerId(upload.getId(), customer.getId()))
                .map(this::toAgingDTO)
                .orElse(null);

        BigDecimal totalPosted = nullToZero(
                paymentRepository.sumAmountByCustomerId(customer.getId()));

        BigDecimal totalUnallocated = nullToZero(
                paymentRepository.sumUnallocatedByCustomer(customer.getId())
                        .orElse(ZERO));

        long postedPaymentCount =
                paymentRepository.countByCustomerId(customer.getId());

        BigDecimal pendingAmount = nullToZero(
                paymentInProgressRepository.sumPendingAmountByCustomerId(
                        customer.getId(),
                        List.of(PaymentStatus.INITIATED,
                                PaymentStatus.AWAITING_CONFIRMATION,
                                PaymentStatus.READY_TO_POST)));

        long pendingPaymentCount =
                paymentInProgressRepository.countByCustomerIdAndStatusIn(
                        customer.getId(),
                        List.of(PaymentStatus.INITIATED,
                                PaymentStatus.AWAITING_CONFIRMATION,
                                PaymentStatus.READY_TO_POST));

        CustomerPaymentSummaryDTO paymentSummary =
                CustomerPaymentSummaryDTO.builder()
                        .totalPosted(totalPosted)
                        .totalUnallocated(totalUnallocated)
                        .totalAllocated(totalPosted.subtract(totalUnallocated))
                        .postedPaymentCount(postedPaymentCount)
                        .pendingAmount(pendingAmount)
                        .pendingPaymentCount(pendingPaymentCount)
                        .build();

        var recentPosted = paymentRepository
                .findTop10ByCustomer_UsernameOrderByPaymentDateDesc(username);

        List<PaymentInProgress> recentPending = paymentInProgressRepository
                .findTop10ByCustomer_UsernameAndStatusInOrderByCreatedAtDesc(
                        username,
                        List.of(PaymentStatus.INITIATED,
                                PaymentStatus.AWAITING_CONFIRMATION,
                                PaymentStatus.READY_TO_POST));

        BigDecimal totalPending = latestAging == null
                ? ZERO
                : nullToZero(latestAging.getTotalDebt());

        return CustomerDashboardDTO.builder()
                .businessName(customer.getBusinessName())
                .customerCode(customer.getCustomerCode())
                .username(customer.getUsername())
                .contactPerson(customer.getContactPerson())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .location(customer.getLocation())
                .unitName(customer.getUnit() != null ? customer.getUnit().getName() : null)
                .townName(customer.getTown() != null ? customer.getTown().getName() : null)
                .regionName(customer.getTown() != null && customer.getTown().getRegion() != null
                        ? customer.getTown().getRegion().getName() : null)
                .active(customer.getActive())
                .latestAging(latestAging)
                .paymentSummary(paymentSummary)
                .recentPostedPayments(recentPosted)
                .recentPendingPayments(recentPending)
                .totalPending(totalPending)
                .totalOverpayments(totalUnallocated)
                .outstandingSalesCount(latestAging != null
                        && latestAging.getTotalDebt().compareTo(ZERO) > 0 ? 1 : 0)
                .build();
    }

    private CustomerAgingDTO toAgingDTO(DebtAgeingRecord record) {
        BigDecimal total = nullToZero(record.getTotalDebt());

        return CustomerAgingDTO.builder()
                .uploadId(record.getUpload().getId())
                .fileName(record.getUpload().getFileName())
                .reportDate(record.getUpload().getReportDate() == null
                        ? null
                        : record.getUpload().getReportDate().toString())
                .currentAmount(nullToZero(record.getCurrentAmount()))
                .days30(nullToZero(record.getDays30()))
                .days60(nullToZero(record.getDays60()))
                .days90(nullToZero(record.getDays90()))
                .days120(nullToZero(record.getDays120()))
                .over120(nullToZero(record.getOver120()))
                .totalDebt(total)
                .currentPercentage(percentage(record.getCurrentAmount(), total))
                .days30Percentage(percentage(record.getDays30(), total))
                .days60Percentage(percentage(record.getDays60(), total))
                .days90Percentage(percentage(record.getDays90(), total))
                .days120Percentage(percentage(record.getDays120(), total))
                .over120Percentage(percentage(record.getOver120(), total))
                .build();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private double percentage(BigDecimal amount, BigDecimal total) {
        amount = nullToZero(amount);
        total = nullToZero(total);

        if (total.compareTo(ZERO) == 0) {
            return 0.0;
        }

        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
