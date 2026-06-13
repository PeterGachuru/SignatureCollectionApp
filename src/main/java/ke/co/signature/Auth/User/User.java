package ke.co.signature.Auth.User;

import jakarta.persistence.*;
import ke.co.signature.Auth.Role.Role;
import ke.co.signature.BaseEntity;
import lombok.Data;

import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // will be stored hashed

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    private boolean enabled = true;

}
