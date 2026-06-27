package ke.co.signature.Payment;

import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.CreditSale.CreditSaleService;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.MpesaIntegration.MpesaTransaction;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentSplit.PaymentSplit;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class PaymentPostingService {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository inProgressRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final PaymentSplitRepository paymentSplitRepository;
    private final CreditSaleService creditSaleService;
    private final CustomerRepository customerRepository;


    @Transactional
    public void postPayment(Long paymentInProgressId) {

        PaymentInProgress pip = inProgressRepository.findById(paymentInProgressId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (pip.getStatus() != PaymentStatus.READY_TO_POST) {
            throw new IllegalStateException("Payment not ready to post");
        }

        PostedPayment postedPayment = new PostedPayment();
        postedPayment.setCustomer(pip.getCustomer());
        postedPayment.setAmount(pip.getAmount());
        postedPayment.setReference(pip.getReference());
        postedPayment.setPhoneNumber(pip.getPhoneNumber());
        postedPayment.setPaymentMode(pip.getPaymentMode());
        postedPayment.setPaymentDate(pip.getPaymentDate());

        paymentRepository.save(postedPayment);

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
            split.setPostedPayment(postedPayment);
            split.setCreditSale(cs);
            split.setAmountApplied(applied);
            paymentSplitRepository.save(split);
            creditSaleService.updateClassification(cs);

            remainingAmount = remainingAmount.subtract(applied);
        }

        // ✅ Record unapplied amount
        postedPayment.setUnallocatedAmount(remainingAmount);
        paymentRepository.save(postedPayment);

        inProgressRepository.delete(pip);
    }

    public void postMpesaPayment(MpesaTransaction tx) {
        Customer customer = customerRepository.findByCustomerCode(tx.getCustomerCode()).get();
        PostedPayment postedPayment = new PostedPayment();
        postedPayment.setCustomer(customer);
        postedPayment.setAmount(tx.getAmount());
        postedPayment.setReference(tx.getMpesaReceiptNumber());
        postedPayment.setPhoneNumber(tx.getPhoneNumber());
        postedPayment.setPaymentMode(PaymentMode.MPESA);
        postedPayment.setPaymentDate(LocalDate.now());

        paymentRepository.save(postedPayment);

        BigDecimal remainingAmount = tx.getAmount();

        List<CreditSale> creditSalesToSettle;

        if (tx.getCreditSale() != null) {

            creditSalesToSettle = new ArrayList<>();
            creditSalesToSettle.add(creditSaleRepository.findById(tx.getCreditSale()).get());

            creditSalesToSettle.addAll(
                    creditSaleRepository
                            .findByCustomerAndBalanceGreaterThanOrderByCreatedAtAsc(
                                    customer, BigDecimal.ZERO
                            )
            );
        } else {
            creditSalesToSettle =
                    creditSaleRepository
                            .findByCustomerAndBalanceGreaterThanOrderByCreatedAtAsc(
                                    customer, BigDecimal.ZERO
                            );
        }

        for (CreditSale cs : creditSalesToSettle) {

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            if (cs.getBalance().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal applied = remainingAmount.min(cs.getBalance());

            cs.setBalance(cs.getBalance().subtract(applied));
            creditSaleRepository.save(cs);

            PaymentSplit split = new PaymentSplit();
            split.setPostedPayment(postedPayment);
            split.setCreditSale(cs);
            split.setAmountApplied(applied);
            paymentSplitRepository.save(split);
            creditSaleService.updateClassification(cs);

            remainingAmount = remainingAmount.subtract(applied);
        }

        // ✅ Record unapplied amount
        postedPayment.setUnallocatedAmount(remainingAmount);
        paymentRepository.save(postedPayment);
    }
}
