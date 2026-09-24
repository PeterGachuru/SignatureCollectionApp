package ke.co.signature.Payment.PaymentSplit;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecord;
import ke.co.signature.Payment.PostedPayment;
import ke.co.signature.Payment.DebtBucket;
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
    @JoinColumn(name = "payment_id", nullable = false)
    private PostedPayment postedPayment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "debt_ageing_record_id", nullable = false)
    private DebtAgeingRecord debtAgeingRecord;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_bucket")
    private DebtBucket bucket;
}