package ke.co.signature.Payment;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Configs.Bank.Bank;
import ke.co.signature.Customer.Customer;
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