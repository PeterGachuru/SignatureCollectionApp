package ke.co.signature.Payment.PaymentInProgress;

import ke.co.signature.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentInProgressRepository
        extends JpaRepository<PaymentInProgress, Long> {

    List<PaymentInProgress> findByStatus(PaymentStatus status);

    List<PaymentInProgress> findAllByOrderByCreatedAtDesc();
    Page<PaymentInProgress> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PaymentInProgress>
    findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            String customerName,
            String reference,
            Pageable pageable
    );
}