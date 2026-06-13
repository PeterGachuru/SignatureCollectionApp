package ke.co.signature.Payment.PaymentSplit;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.Payment.Payment;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_splits")
@Data
public class PaymentSplit extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "credit_sale_id")
    private CreditSale creditSale;

    @Column(nullable = false)
    private BigDecimal amountApplied;
}

