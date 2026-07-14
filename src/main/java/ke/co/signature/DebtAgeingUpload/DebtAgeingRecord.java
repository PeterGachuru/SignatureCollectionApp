package ke.co.signature.DebtAgeingUpload;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Customer.Customer;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class DebtAgeingRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private DebtAgeingUpload upload;

    @ManyToOne(optional = false)
    private Customer customer;

    private BigDecimal currentAmount;

    private BigDecimal days30;

    private BigDecimal days60;

    private BigDecimal days90;

    private BigDecimal days120;

    private BigDecimal over120;

    private BigDecimal totalDebt;

    private String remarks;
}