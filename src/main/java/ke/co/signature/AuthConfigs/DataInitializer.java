package ke.co.signature.AuthConfigs;

import ke.co.signature.Auth.Role.Role;
import ke.co.signature.Auth.Role.RoleRepository;
import ke.co.signature.Auth.User.User;
import ke.co.signature.Auth.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

import static ke.co.signature.Auth.Role.RoleValue.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createRoles();
        createAdminUser();
    }

    void createRoles() {
        roleRepository.findByName(ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ROLE_ADMIN);
                    return roleRepository.save(role);
                });
        roleRepository.findByName(ROLE_CUSTOMER_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ROLE_CUSTOMER_ADMIN);
                    return roleRepository.save(role);
                });
        roleRepository.findByName(ROLE_REGIONAL_REP)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ROLE_REGIONAL_REP);
                    return roleRepository.save(role);
                });
    }

    void createAdminUser() {
        if (userRepository.count() > 0) {
            return; // users already exist
        }

        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ROLE_ADMIN);
                    return roleRepository.save(role);
                });

        System.out.println("Admin role: "+adminRole);

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole)); // ✅ THIS replaces setRole()

        userRepository.save(admin);

        System.out.println("✅ Default admin user created: admin / admin123");
    }
}
