package ke.co.signature.Payment.PaymentInProgress;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Configs.Bank.Bank;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Payment.PaymentEntrySource;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * M-Pesa receipt, bank reference, etc.
     */
    private String reference;

    /**
     * Only applicable to M-Pesa.
     */
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDate paymentDate;

    /** Source of the payment entry (administrator, customer, or M-Pesa callback). */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source")
    private PaymentEntrySource entrySource;

    /*
     * CHEQUE INFORMATION
     */

    @ManyToOne
    @JoinColumn(name = "bank_id")
    private Bank bank;

    private String chequeNumber;

    private LocalDate chequeDate;

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