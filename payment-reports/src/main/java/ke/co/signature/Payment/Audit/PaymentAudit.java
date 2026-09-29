package ke.co.signature.Payment.Audit;

import jakarta.persistence.*;
import ke.co.signature.Auth.User.User;
import ke.co.signature.BaseEntity;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Payment.PaymentMode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_audits")
@Data
public class PaymentAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentAuditAction action;

    /** The id of the PostedPayment before it was removed. */
    private Long originalPaymentId;

    /** The id of the recreated PaymentInProgress transaction. */
    private Long paymentInProgressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private String reference;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private LocalDate paymentDate;

    @Column(name = "status_before", length = 30)
    private String statusBefore;

    /** Amount of debt balance restored by the unpost. Zero for already-reversed payments. */
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceImpact = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by_id")
    private User actionBy;

    @Column(nullable = false)
    private LocalDateTime actionAt;

    @Column(length = 1000)
    private String details;

    @PrePersist
    public void onAuditCreate() {
        if (actionAt == null) {
            actionAt = LocalDateTime.now();
        }
    }
}
