package ke.co.signature.DebtClassification;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "debt_classifications")
@Data
public class DebtClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // inclusive
    @Column(nullable = false)
    private Integer minDaysLate;

    // inclusive, nullable = open-ended (181+)
    private Integer maxDaysLate;

    @Column(nullable = false)
    private Boolean active = true;
}
