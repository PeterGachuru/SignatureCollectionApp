package ke.co.signature.Payment;


import ke.co.signature.BaseEntity;
import ke.co.signature.CreditSale.DebtStatus;
import ke.co.signature.Customer.Customer;
import ke.co.signature.CreditSale.CreditSale;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Customer who made the payment
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ✅ Optional: payment can be tied to a specific credit sale
    @ManyToOne
    @JoinColumn(name = "credit_sale_id")
    private CreditSale creditSale;

    @Column(nullable = false)
    private BigDecimal amount;

    // MPESA receipt number (or manual reference)
    private String reference;

    // Phone number used for payment
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    // Date the payment was made (business date)
    @Column(nullable = false)
    private LocalDate paymentDate;

    // ✅ Amount not yet allocated to any credit sale
    @Column(nullable = false)
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    @PrePersist
    public void onCreate() {
        super.onCreate();
        System.out.println("In oncreate");
        if (this.paymentDate == null) {
            this.paymentDate = LocalDate.now();
        }
    }
}
