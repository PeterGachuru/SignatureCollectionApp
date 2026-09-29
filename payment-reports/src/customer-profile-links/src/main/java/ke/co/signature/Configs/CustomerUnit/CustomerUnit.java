package ke.co.signature.Configs.CustomerUnit;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import lombok.Data;

@Entity
@Table(name = "customer_units")
@Data
public class CustomerUnit  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // Example: Hospitals and Clinics, Chemist and Pharmacy

    // getters & setters
}
