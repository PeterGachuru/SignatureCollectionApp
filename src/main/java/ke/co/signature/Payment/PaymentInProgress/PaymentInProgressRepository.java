package ke.co.signature.Payment.PaymentInProgress;

import ke.co.signature.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentInProgressRepository
        extends JpaRepository<PaymentInProgress, Long> {

    List<PaymentInProgress> findByStatus(PaymentStatus status);

    List<PaymentInProgress> findAllByOrderByCreatedAtDesc();
}
