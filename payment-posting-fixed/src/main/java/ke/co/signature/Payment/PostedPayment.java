package ke.co.signature.Payment;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Configs.Bank.Bank;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Auth.User.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
public class PostedPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String reference;

    /**
     * Only applicable to M-Pesa.
     */
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false)
    private LocalDate paymentDate;

    /** Source of the payment entry (administrator, customer, or M-Pesa callback). */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source")
    private PaymentEntrySource entrySource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'POSTED'")
    private PostedPaymentStatus status = PostedPaymentStatus.POSTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private User reversedBy;

    private java.time.LocalDateTime reversedAt;

    @Column(length = 1000)
    private String reversalReason;

    @Column(precision = 19, scale = 2)
    private BigDecimal reversalBalanceImpact = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    /*
     * CHEQUE INFORMATION
     */

    @ManyToOne
    @JoinColumn(name = "bank_id")
    private Bank bank;

    private String chequeNumber;

    private LocalDate chequeDate;

    @PrePersist
    public void onCreate() {

        super.onCreate();

        if (this.paymentDate == null) {
            this.paymentDate = LocalDate.now();
        }

        if (this.unallocatedAmount == null) {
            this.unallocatedAmount = BigDecimal.ZERO;
        }
    }
}