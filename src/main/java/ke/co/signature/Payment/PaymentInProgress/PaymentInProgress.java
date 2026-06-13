package ke.co.signature.Payment.PaymentInProgress;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Payment.PaymentMode;
import ke.co.signature.Payment.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments_in_progress")
@Data
public class PaymentInProgress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer is mandatory
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Optional: linked credit sale
    @ManyToOne
    @JoinColumn(name = "credit_sale_id")
    private CreditSale creditSale;

    @Column(nullable = false)
    private BigDecimal amount;

    private String reference; // MPESA receipt or manual reference

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    // INITIATED, AWAITING_CONFIRMATION, FAILED, READY_TO_POST

    @Column(nullable = false)
    private LocalDate paymentDate;

    private String failureReason;

    @PrePersist
    public void onCreate() {
        super.onCreate();
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
        if (status == null) {
            status = PaymentStatus.INITIATED;
        }
    }
}
