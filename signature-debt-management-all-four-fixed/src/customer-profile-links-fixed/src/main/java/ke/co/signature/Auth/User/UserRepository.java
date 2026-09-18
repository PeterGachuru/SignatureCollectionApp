package ke.co.signature.Auth.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        JOIN u.roles r
        WHERE r.name = :role
    """)
    long countUsersWithRole(@Param("role") String role);
}
