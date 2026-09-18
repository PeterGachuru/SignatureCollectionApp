package ke.co.signature.CreditSale;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Customer.Customer;
import ke.co.signature.DebtClassification.DebtClassification;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_sales")
@Data
public class CreditSale extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String saleCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    private BigDecimal grossAmount;

    @Column(nullable = false)
    private LocalDate saleDate = LocalDate.now();

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private BigDecimal balance;


    @Column(nullable = false)
    private BigDecimal discount;


    @Column(nullable = false)
    private BigDecimal vatIncluded;


    @Column(nullable = false)
    private BigDecimal invoiceAmount;

    @ManyToOne
    @JoinColumn(name = "classification_id")
    private DebtClassification classification;

    @Enumerated(EnumType.STRING)
    private DebtStatus status = DebtStatus.CURRENT;
}
