package ke.co.signature.Configs.Bank;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import lombok.Data;

@Entity
@Table(
        name = "banks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bank_code",
                        columnNames = "code"
                )
        }
)
@Data
public class Bank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}