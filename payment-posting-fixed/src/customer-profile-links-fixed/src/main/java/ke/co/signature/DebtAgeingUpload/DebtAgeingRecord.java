package ke.co.signature.DebtAgeingUpload;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Customer.Customer;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(
        name = "debt_ageing_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ageing_upload_customer",
                        columnNames = {
                                "upload_id",
                                "customer_id"
                        }
                )
        }
)
@Data
public class DebtAgeingRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private DebtAgeingUpload upload;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal days30 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal days60 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal days90 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal days120 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal over120 = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebt = BigDecimal.ZERO;

    private String remarks;
}