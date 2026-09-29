package ke.co.signature.Payment.Audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
    Page<PaymentAudit> findByActionOrderByActionAtDesc(PaymentAuditAction action, Pageable pageable);
}
