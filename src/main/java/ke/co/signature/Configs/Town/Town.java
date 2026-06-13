package ke.co.signature.Configs.Town;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Configs.Region.Region;
import lombok.Data;

@Entity
@Table(name = "towns")
@Data
public class Town extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
}
