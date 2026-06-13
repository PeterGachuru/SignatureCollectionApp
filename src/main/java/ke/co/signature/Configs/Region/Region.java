package ke.co.signature.Configs.Region;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import lombok.Data;

@Entity
@Table(name = "regions")
@Data
public class Region  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // getters & setters
}

