package ke.co.signature.Payment;

import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.CreditSale.DebtStatus;
import ke.co.signature.Customer.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CreditSaleRepository creditSaleRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          CreditSaleRepository creditSaleRepository) {
        this.paymentRepository = paymentRepository;
        this.creditSaleRepository = creditSaleRepository;
    }

    /**
     * Records a payment and reduces the outstanding balance of a credit sale.
     * This method should be called after successful MPESA STK callback.
     */
    @Transactional
    public Payment applyMpesaPayment(CreditSale creditSale,
                                     BigDecimal amountPaid,
                                     String mpesaReceipt,
                                     String phone) {

        // 🔒 Validate
        if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (creditSale.getBalance().compareTo(amountPaid) < 0) {
            throw new IllegalArgumentException("Payment exceeds outstanding balance");
        }

        // ✅ Create payment record
        Payment payment = new Payment();
        payment.setCustomer(creditSale.getCustomer());
        payment.setCreditSale(creditSale);
        payment.setAmount(amountPaid);
        payment.setReference(mpesaReceipt);
        payment.setPhoneNumber(phone);
        payment.setPaymentMode(PaymentMode.MPESA);
        payment.setPaymentDate(LocalDate.now());

        paymentRepository.save(payment);

        // ✅ Reduce balance
        creditSale.setBalance(
                creditSale.getBalance().subtract(amountPaid)
        );

        // Optional: mark as fully paid
        if (creditSale.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            creditSale.setStatus(DebtStatus.PAID);
        }

        creditSaleRepository.save(creditSale);

        return payment;
    }

    public BigDecimal getCustomerUnallocatedAmount(Customer customer) {
        return paymentRepository.sumUnallocatedByCustomer(customer.getId())
                .orElse(BigDecimal.ZERO);
    }
}
