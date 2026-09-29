package ke.co.signature.Auth.Role;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import lombok.Data;

@Entity
@Table(name = "roles")
@Data
public class Role  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @Enumerated(EnumType.STRING)
    private RoleValue name; // e.g., ROLE_ADMIN, ROLE_USER

}
