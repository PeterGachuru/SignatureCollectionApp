package ke.co.signature.Payment;

import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.CreditSale.CreditSaleService;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentSplit.PaymentSplit;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PaymentPostingService {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository inProgressRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final PaymentSplitRepository paymentSplitRepository;
    private final CreditSaleService creditSaleService;


    @Transactional
    public void postPayment(Long paymentInProgressId) {

        PaymentInProgress pip = inProgressRepository.findById(paymentInProgressId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (pip.getStatus() != PaymentStatus.READY_TO_POST) {
            throw new IllegalStateException("Payment not ready to post");
        }

        Payment payment = new Payment();
        payment.setCustomer(pip.getCustomer());
        payment.setAmount(pip.getAmount());
        payment.setReference(pip.getReference());
        payment.setPhoneNumber(pip.getPhoneNumber());
        payment.setPaymentMode(pip.getPaymentMode());
        payment.setPaymentDate(pip.getPaymentDate());

        paymentRepository.save(payment);

        BigDecimal remainingAmount = pip.getAmount();

        List<CreditSale> creditSalesToSettle;

        if (pip.getCreditSale() != null) {
            creditSalesToSettle = new ArrayList<>();
            creditSalesToSettle.add(pip.getCreditSale());

            creditSalesToSettle.addAll(
                    creditSaleRepository
                            .findByCustomerAndBalanceGreaterThanOrderByCreatedAtAsc(
                                    pip.getCustomer(), BigDecimal.ZERO
                            )
            );
        } else {
            creditSalesToSettle =
                    creditSaleRepository
                            .findByCustomerAndBalanceGreaterThanOrderByCreatedAtAsc(
                                    pip.getCustomer(), BigDecimal.ZERO
                            );
        }

        for (CreditSale cs : creditSalesToSettle) {

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            if (cs.getBalance().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal applied = remainingAmount.min(cs.getBalance());

            cs.setBalance(cs.getBalance().subtract(applied));
            creditSaleRepository.save(cs);

            PaymentSplit split = new PaymentSplit();
            split.setPayment(payment);
            split.setCreditSale(cs);
            split.setAmountApplied(applied);
            paymentSplitRepository.save(split);
            creditSaleService.updateClassification(cs);

            remainingAmount = remainingAmount.subtract(applied);
        }

        // ✅ Record unapplied amount
        payment.setUnallocatedAmount(remainingAmount);
        paymentRepository.save(payment);

        inProgressRepository.delete(pip);
    }
}
