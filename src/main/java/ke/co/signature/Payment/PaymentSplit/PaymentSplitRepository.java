package ke.co.signature.Payment.PaymentSplit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentSplitRepository
        extends JpaRepository<PaymentSplit, Long> {

    List<PaymentSplit>
    findByDebtAgeingRecordId(Long debtAgeingRecordId);

    List<PaymentSplit>
    findByPostedPaymentId(Long postedPaymentId);
}