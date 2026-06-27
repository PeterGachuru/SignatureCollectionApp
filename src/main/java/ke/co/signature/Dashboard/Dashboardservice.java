package ke.co.signature.Dashboard;

import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.Payment.PostedPayment;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class Dashboardservice {

    private final CustomerRepository customerRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final PaymentRepository paymentRepository;

    private final PaymentInProgressRepository paymentInProgressRepository;

    public Dashboardservice(CustomerRepository customerRepository,
                            CreditSaleRepository creditSaleRepository,
                            PaymentRepository paymentRepository,
                            PaymentInProgressRepository paymentInProgressRepository) {
        this.customerRepository = customerRepository;
        this.creditSaleRepository = creditSaleRepository;
        this.paymentRepository = paymentRepository;
        this.paymentInProgressRepository = paymentInProgressRepository;
    }

    public DashboardSummaryDTO getDashboardSummary() {


        // ✅ Total customers
        long totalCustomers = customerRepository.count();

        LocalDate cutoffDate = LocalDate.now().minusDays(90);

        Date cutoff = Date.from(
                cutoffDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );

        var recentCreditSales = creditSaleRepository.findByCreatedAtAfter(cutoff);;

//         ✅ Active customers (distinct customers with recent credit)
        Set<Long> activeCustomerIds = recentCreditSales.stream()
                .map(cs -> cs.getCustomer().getId())
                .collect(Collectors.toSet());

        long activeCustomers90Days = activeCustomerIds.size();

        // ✅ Total due loans (all time)
        BigDecimal totalDueLoans = creditSaleRepository.findAll().stream()
                .map(CreditSale::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Due loans from last 90 days
        BigDecimal totalDueLoans90Days = recentCreditSales.stream()
                .map(CreditSale::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Payments in last 90 days
        BigDecimal totalPayments90Days = paymentRepository.findByPaymentDateAfter(cutoffDate)
                .stream()
                .map(PostedPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);



        List<Object[]> results = creditSaleRepository.getClassificationSummary();

        List<DebtClassificationSummaryDTO> classificationSummary = results.stream()
                .map(r -> new DebtClassificationSummaryDTO(
                        (String) r[0],
                        (Integer) r[1],
                        (Long) r[2],
                        (BigDecimal) r[3]
                ))
                .toList();

        return new DashboardSummaryDTO(
                paymentInProgressRepository.count(),
                totalCustomers,
                activeCustomers90Days,
                totalDueLoans,
                totalDueLoans90Days.doubleValue(),
                totalPayments90Days,
                paymentRepository.getTotalOverpayments(),
                classificationSummary   // 👈 NEW
        );
    }

}
