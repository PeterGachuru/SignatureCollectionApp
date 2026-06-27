package ke.co.signature.CustomerDashboard;

import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.Payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

    private final CreditSaleRepository creditSaleRepo;
    private final PaymentRepository paymentRepo;

    public CustomerDashboardDTO getDashboard(String username) {

        return CustomerDashboardDTO.builder()
                .totalPending(creditSaleRepo.totalPendingBalance(username))
                .totalOverpayments(paymentRepo.totalOverpayments(username))
                .outstandingSalesCount(
                        creditSaleRepo.countByCustomer_UsernameAndBalanceGreaterThan(
                                username, BigDecimal.ZERO))
                .recentPostedPayments(
                        paymentRepo.findTop5ByCustomer_UsernameOrderByPaymentDateDesc(username))
                .recentPurchases(
                        creditSaleRepo.findTop5ByCustomer_UsernameOrderByIdDesc(username))
                .build();
    }
}
