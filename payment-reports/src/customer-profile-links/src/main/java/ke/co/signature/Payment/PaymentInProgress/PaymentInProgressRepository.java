package ke.co.signature.Payment.PaymentInProgress;

import ke.co.signature.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentInProgressRepository
        extends JpaRepository<PaymentInProgress, Long> {

    List<PaymentInProgress> findByStatus(PaymentStatus status);

    List<PaymentInProgress> findAllByOrderByCreatedAtDesc();
    Page<PaymentInProgress> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCustomerIdAndStatusIn(
            Long customerId,
            List<PaymentStatus> statuses
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentInProgress p
        WHERE p.customer.id = :customerId
        AND p.status IN :statuses
    """)
    BigDecimal sumPendingAmountByCustomerId(
            @Param("customerId") Long customerId,
            @Param("statuses") List<PaymentStatus> statuses
    );

    List<PaymentInProgress> findTop10ByCustomer_UsernameAndStatusInOrderByCreatedAtDesc(
            String username,
            List<PaymentStatus> statuses
    );

    Page<PaymentInProgress>
    findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            String customerName,
            String reference,
            Pageable pageable
    );
}