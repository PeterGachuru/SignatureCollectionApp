package ke.co.signature.Auth.User;

import ke.co.signature.Auth.Role.Role;
import ke.co.signature.Auth.Role.RoleRepository;
import ke.co.signature.Auth.Role.RoleValue;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashSet;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Admin creates a user
    public User createUser(String username, String rawPassword, RoleValue roleName) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        System.out.println("=========================New user================================");
        System.out.println("Username: "+username);
        System.out.println("Password: "+rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        return userRepo.save(user);
    }

    public String generateEasyPassword(String username) {
        SecureRandom random = new SecureRandom();
        String symbols = "!@#";
        String digits = "23456789";

        char first = username.charAt(random.nextInt(username.length()));
        char second = username.charAt(random.nextInt(username.length()));
        char digit = digits.charAt(random.nextInt(digits.length()));
        char symbol = symbols.charAt(random.nextInt(symbols.length()));

        return "" + first + digit + second + symbol;
    }

    public boolean userExists(String username) {
        return userRepo.findByUsername(username).isPresent();
    }
}
